package io.github.zero88.msa.sql.service;

import io.github.zero88.msa.sql.CompositeMetadata;
import io.github.zero88.msa.sql.handler.EntityHandler;
import io.github.zero88.msa.sql.EntityMetadata;
import io.github.zero88.msa.sql.service.transformer.GroupEntityTransformer;
import io.github.zero88.msa.sql.service.decorator.GroupRequestDecorator;
import io.github.zero88.msa.sql.marker.GroupReferencingEntityMarker;
import io.github.zero88.msa.sql.pojos.CompositePojo;
import io.github.zero88.msa.sql.query.GroupQueryExecutor;

import lombok.NonNull;

/**
 * Abstract service to implement {@code CRUD} listeners for the {@code Group entity}.
 *
 * @param <M>  Type of {@code EntityMetadata}
 * @param <CP> Type of {@code CompositePojo}
 * @param <CM> Type of {@code CompositeMetadata}
 * @see EntityMetadata
 * @see CompositePojo
 * @see CompositeMetadata
 * @see GroupEntityService
 * @see GroupRequestDecorator
 * @see GroupEntityTransformer
 * @since 1.0.0
 */
public abstract class AbstractGroupEntityService<M extends EntityMetadata, CP extends CompositePojo,
                                                    CM extends CompositeMetadata>
    extends io.github.zero88.msa.sql.service.AbstractReferencingEntityService<CP, CM>
    implements GroupEntityService<M, CP, CM>, GroupRequestDecorator, GroupEntityTransformer {

    /**
     * Instantiates a new Abstract group entity service.
     *
     * @param entityHandler the entity handler
     * @since 1.0.0
     */
    public AbstractGroupEntityService(@NonNull EntityHandler entityHandler) {
        super(entityHandler);
    }

    @Override
    public @NonNull GroupQueryExecutor<CP> queryExecutor() {
        return GroupEntityService.super.queryExecutor();
    }

    @Override
    public @NonNull GroupEntityTransformer transformer() {
        return this;
    }

    @Override
    public GroupReferencingEntityMarker marker() {
        return this;
    }

}
