package io.github.zero88.msa.sql.workflow.step;

import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

import io.github.zero88.msa.bp.dto.msg.RequestData;
import io.github.zero88.msa.sql.pojos.DMLPojo;
import io.github.zero88.msa.sql.validation.OperationValidator;

import lombok.NonNull;

/**
 * Represents a {@code DML} step on the entity resources in batch
 *
 * @since 1.0.0
 */
public interface DMLBatchStep extends SQLBatchStep {

    /**
     * Execute {@code SQL manipulate command} based on given {@code request data} and {@code validator}.
     *
     * @param reqData   the req data
     * @param validator the validator
     * @return json result in Single
     * @see RequestData
     * @see OperationValidator
     * @see DMLPojo
     * @since 1.0.0
     */
    Single<JsonObject> execute(@NonNull RequestData reqData, @NonNull OperationValidator validator);

}
