package io.github.zero88.qwe.sql.handler;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jooq.Catalog;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.UpdatableRecord;

import io.github.jklingsporn.vertx.jooq.rx.VertxDAO;
import io.github.jklingsporn.vertx.jooq.rx.jdbc.JDBCRXGenericQueryExecutor;
import io.github.jklingsporn.vertx.jooq.shared.internal.VertxPojo;
import io.github.zero88.qwe.component.HasSharedData;
import io.github.zero88.qwe.dto.JsonData;
import io.github.zero88.qwe.event.EventbusClient;
import io.github.zero88.qwe.sql.EntityMetadata;
import io.github.zero88.qwe.sql.MetadataIndex;
import io.github.zero88.qwe.sql.schema.SchemaHandler;
import io.github.zero88.qwe.sql.query.ComplexQueryExecutor;
import io.github.zero88.utils.Reflections.ReflectionClass;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import lombok.NonNull;

/**
 * Represents for Entity handler.
 *
 * @since 1.0.0
 */
public interface EntityHandler extends HasSharedData {

    /**
     * Parse pojo.
     *
     * @param <P>        Type of {@code VertxPojo}
     * @param modelClass the model class
     * @param pojo       the pojo
     * @return the pojo
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    static <P extends VertxPojo> P parse(@NonNull Class<P> modelClass, @NonNull JsonObject pojo) {
        return (P) ReflectionClass.createObject(modelClass).fromJson(pojo);
    }

    /**
     * Parse pojo.
     *
     * @param <P>       Type of {@code VertxPojo}
     * @param pojoClass the pojo class
     * @param data      the data
     * @return the pojo
     * @since 1.0.0
     */
    static <P extends VertxPojo> P parse(@NonNull Class<P> pojoClass, @NonNull Object data) {
        return parse(pojoClass, JsonData.tryParse(data).toJson());
    }

    /**
     * Defines database catalog.
     *
     * @return the catalog
     * @since 1.0.0
     */
    @NonNull Catalog catalog();

    /**
     * Get Vertx.
     *
     * @return the vertx
     * @since 1.0.0
     */
    @NonNull Vertx vertx();

    /**
     * Get eventbus client.
     *
     * @return the eventbus client
     * @see EventbusClient
     * @since 1.0.0
     */
    @NonNull EventbusClient eventClient();

    /**
     * Data dir path.
     *
     * @return the path
     * @since 1.0.0
     */
    @NonNull Path dataDir();

    /**
     * Get {@code dsl context}.
     *
     * @return the dsl context
     * @see DSLContext
     * @since 1.0.0
     */
    @NonNull DSLContext dsl();

    /**
     * Create DAO by given entity handler
     *
     * @param <K>      Type of {@code primary key}
     * @param <P>      Type of {@code VertxPojo}
     * @param <R>      Type of {@code UpdatableRecord}
     * @param <D>      Type of {@code VertxDAO}
     * @param metadata Entity metadata
     * @return the instance of DAO
     * @since 1.0.0
     */
    default @NonNull <K, P extends VertxPojo, R extends UpdatableRecord<R>, D extends VertxDAO<R, P, K>> D dao(
        @NonNull EntityMetadata<K, P, R, D> metadata) {
        return dao(metadata.daoClass());
    }

    /**
     * Create {@code DAO} by given {@code daoClass}.
     *
     * @param <K>      Type of {@code primary key}
     * @param <P>      Type of {@code VertxPojo}
     * @param <R>      Type of {@code UpdatableRecord}
     * @param <D>      Type of {@code VertxDAO}
     * @param daoClass the dao class
     * @return the instance of DAO
     * @since 1.0.0
     */
    default <K, P extends VertxPojo, R extends UpdatableRecord<R>, D extends VertxDAO<R, P, K>> D dao(
        @NonNull Class<D> daoClass) {
        return dao(dsl().configuration(), daoClass);
    }

    /**
     * Create {@code DAO} by given {@code daoClass} and given {@code jooq configuration}.
     *
     * @param <K>           Type of {@code primary key}
     * @param <P>           Type of {@code VertxPojo}
     * @param <R>           Type of {@code UpdatableRecord}
     * @param <D>           Type of {@code VertxDAO}
     * @param configuration the configuration
     * @param daoClass      the dao class
     * @return the d
     * @since 1.0.0
     */
    default <K, P extends VertxPojo, R extends UpdatableRecord<R>, D extends VertxDAO<R, P, K>> D dao(
        @NonNull Configuration configuration, @NonNull Class<D> daoClass) {
        Map<Class, Object> input = new LinkedHashMap<>();
        input.put(Configuration.class, configuration);
        input.put(io.vertx.reactivex.core.Vertx.class, io.vertx.reactivex.core.Vertx.newInstance(vertx()));
        return ReflectionClass.createObject(daoClass, input);
    }

    /**
     * Get generic query executor.
     *
     * @return the generic query executor
     * @see JDBCRXGenericQueryExecutor
     * @since 1.0.0
     */
    default @NonNull JDBCRXGenericQueryExecutor genericQuery() {
        return genericQuery(dsl().configuration());
    }

    /**
     * Get generic query executor.
     *
     * @param configuration the configuration
     * @return the generic query executor
     * @see JDBCRXGenericQueryExecutor
     * @since 1.0.0
     */
    default @NonNull JDBCRXGenericQueryExecutor genericQuery(@NonNull Configuration configuration) {
        return new JDBCRXGenericQueryExecutor(configuration, io.vertx.reactivex.core.Vertx.newInstance(vertx()));
    }

    /**
     * Get complex query executor.
     *
     * @return the complex query executor
     * @see ComplexQueryExecutor
     * @since 1.0.0
     */
    default @NonNull ComplexQueryExecutor complexQuery() {
        return ComplexQueryExecutor.create(this);
    }

    /**
     * Execute any task before setup database
     *
     * @return single of reference to this, so the API can be used fluently
     * @since 1.0.0
     */
    default @NonNull Single<EntityHandler> before() {
        return Single.just(this);
    }

    /**
     * Get {@code Schema handler}.
     *
     * @return the schema handler
     * @see SchemaHandler
     * @since 1.0.0
     */
    @NonNull SchemaHandler schemaHandler();

    /**
     * Gets {@code entity constraint holder}.
     *
     * @return the entity constraint holder. Default is {@link EntityConstraintHolder#BLANK}
     * @see EntityConstraintHolder
     * @since 1.0.0
     */
    default @NonNull EntityConstraintHolder holder() {
        return EntityConstraintHolder.BLANK;
    }

    /**
     * Gets {@code Metadata index}.
     *
     * @return the metadata index. Default is {@link MetadataIndex#BLANK}
     * @see MetadataIndex
     * @since 1.0.0
     */
    default @NonNull MetadataIndex metadataIndex() {
        return MetadataIndex.BLANK;
    }

}
