package io.github.zero88.qwe.sql.workflow.task;

import io.github.jklingsporn.vertx.jooq.shared.internal.VertxPojo;

import io.github.zero88.qwe.transport.Transporter;
import io.github.zero88.qwe.workflow.ProxyTask;

public interface ProxyEntityTask<DC extends EntityDefinitionContext, P extends VertxPojo, R, T extends Transporter>
    extends EntityTask<DC, P, R>, ProxyTask<DC, EntityRuntimeContext<P>, R, T> {

}
