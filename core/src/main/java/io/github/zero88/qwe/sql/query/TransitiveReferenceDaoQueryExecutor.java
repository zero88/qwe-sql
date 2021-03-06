package io.github.zero88.qwe.sql.query;

import java.util.AbstractMap.SimpleEntry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.jooq.DSLContext;
import org.jooq.ResultQuery;
import org.jooq.UpdatableRecord;

import io.github.jklingsporn.vertx.jooq.rx.VertxDAO;
import io.github.jklingsporn.vertx.jooq.shared.internal.VertxPojo;
import io.github.zero88.qwe.dto.JsonData;
import io.github.zero88.qwe.dto.jpa.Pagination;
import io.github.zero88.qwe.dto.msg.RequestData;
import io.github.zero88.qwe.dto.msg.RequestFilter;
import io.github.zero88.qwe.utils.JsonUtils;
import io.github.zero88.qwe.sql.handler.EntityHandler;
import io.github.zero88.qwe.sql.EntityMetadata;
import io.github.zero88.qwe.sql.marker.EntityReferences;
import io.github.zero88.qwe.sql.marker.TransitiveReferenceMarker;
import io.github.zero88.qwe.sql.marker.TransitiveReferenceMarker.TransitiveEntity;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

import lombok.NonNull;

@SuppressWarnings("unchecked")
final class TransitiveReferenceDaoQueryExecutor<K, P extends VertxPojo, R extends UpdatableRecord<R>,
                                                   D extends VertxDAO<R, P, K>>
    extends SimpleDaoQueryExecutor<K, P, R, D> implements TransitiveReferenceQueryExecutor<P> {

    private final TransitiveReferenceMarker marker;

    TransitiveReferenceDaoQueryExecutor(@NonNull EntityHandler handler, @NonNull EntityMetadata<K, P, R, D> metadata,
                                        @NonNull TransitiveReferenceMarker marker) {
        super(handler, metadata);
        this.marker = marker;
    }

    @Override
    public @NonNull TransitiveReferenceMarker marker() {
        return marker;
    }

    @Override
    public Observable<P> findMany(RequestData reqData) {
        final Pagination paging = Optional.ofNullable(reqData.pagination()).orElse(Pagination.builder().build());
        final Function<DSLContext, ResultQuery<R>> viewFunc
            = (Function<DSLContext, ResultQuery<R>>) queryBuilder().view(reqData.filter(), reqData.sort(), paging);
        return checkReferenceExistence(reqData).flatMapObservable(
            ignore -> dao(metadata()).queryExecutor().findMany(viewFunc).flattenAsObservable(rs -> rs));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Single<P> findOneByKey(RequestData reqData) {
        K pk = metadata().parseKey(reqData);
        final Function<DSLContext, ResultQuery<R>> viewOneFunc
            = (Function<DSLContext, ResultQuery<R>>) queryBuilder().viewOne(reqData.filter(), reqData.sort());
        return checkReferenceExistence(reqData).flatMap(i -> dao(metadata()).queryExecutor()
                                                                            .findOne(viewOneFunc)
                                                                            .flatMap(o -> o.map(Single::just)
                                                                                           .orElse(Single.error(
                                                                                               metadata().notFound(
                                                                                                   pk))))
                                                                            .onErrorResumeNext(
                                                                                EntityQueryExecutor::sneakyThrowDBError));
    }

    @Override
    public Single<Boolean> checkReferenceExistence(@NonNull RequestData reqData) {
        final EntityReferences references = marker().referencedEntities();
        final QueryBuilder queryBuilder = queryBuilder();
        return references.toObservable().flatMapSingle(entry -> {
            final EntityMetadata refMeta = entry.getKey();
            final String refField = entry.getValue();
            final Object key = findReferenceKey(reqData, refMeta, refField);
            final TransitiveEntity transitive = marker().transitiveReferences().get(refMeta);
            if (Objects.isNull(key)) {
                return Single.just(true);
            }
            if (Objects.isNull(transitive)) {
                return fetchExists(queryBuilder.exist(refMeta, key)).switchIfEmpty(Single.error(refMeta.notFound(key)));
            }
            return checkTransitiveExistence(reqData, refMeta, key, transitive);
        }).all(aBoolean -> aBoolean);
    }

    private Single<Boolean> checkTransitiveExistence(@NonNull RequestData reqData, @NonNull EntityMetadata reference,
                                                     @NonNull Object referenceKey,
                                                     @NonNull TransitiveEntity transitive) {
        final QueryBuilder queryBuilder = queryBuilder();
        final EntityMetadata context = transitive.getContext();
        final String refField = context.equals(reference) ? reference.jsonKeyName() : reference.requestKeyName();
        return transitive.getReferences()
                         .toObservable()
                         .flatMap(ent -> Optional.ofNullable(findReferenceKey(reqData, ent.getKey(), ent.getValue()))
                                                 .map(tk -> Observable.just(new SimpleEntry<>(ent.getValue(), tk)))
                                                 .orElseGet(Observable::empty))
                         .collectInto(new JsonObject(),
                                      (json, obj) -> json.put(obj.getKey(), JsonData.checkAndConvert(obj.getValue())))
                         .map(json -> json.put(refField, JsonData.checkAndConvert(referenceKey)))
                         .map(RequestFilter::new)
                         .flatMap(filter -> fetchExists(queryBuilder.exist(context, filter)).switchIfEmpty(
                             Single.error(reference.notFound(JsonUtils.kvMsg(filter)))));
    }

}
