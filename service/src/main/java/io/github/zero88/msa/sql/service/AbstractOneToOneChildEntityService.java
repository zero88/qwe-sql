package io.github.zero88.msa.sql.service;

import java.util.Collection;

import io.github.jklingsporn.vertx.jooq.shared.internal.VertxPojo;

import io.github.zero88.msa.bp.event.EventAction;
import io.github.zero88.msa.sql.handler.EntityHandler;
import io.github.zero88.msa.sql.EntityMetadata;
import io.github.zero88.msa.sql.service.decorator.HasReferenceRequestDecorator;

import lombok.NonNull;

/**
 * Abstract service to implement {@code CRUD} listeners for the {@code database entity} has a {@code one-to-one}
 * relationship, and is {@code child} role.
 *
 * @param <P> Type of {@code VertxPojo}
 * @param <M> Type of {@code EntityMetadata}
 * @since 1.0.0
 */
public abstract class AbstractOneToOneChildEntityService<P extends VertxPojo, M extends EntityMetadata>
    extends io.github.zero88.msa.sql.service.AbstractReferencingEntityService<P, M>
    implements HasReferenceRequestDecorator, io.github.zero88.msa.sql.service.OneToOneChildEntityService<P, M> {

    /**
     * Instantiates a new Abstract one to many entity service.
     *
     * @param entityHandler the entity handler
     * @since 1.0.0
     */
    public AbstractOneToOneChildEntityService(@NonNull EntityHandler entityHandler) {
        super(entityHandler);
    }

    @Override
    public @NonNull Collection<EventAction> getAvailableEvents() {
        return io.github.zero88.msa.sql.service.OneToOneChildEntityService.super.getAvailableEvents();
    }

}
