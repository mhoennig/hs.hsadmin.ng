// Licensed under Apache-2.0
package org.hostsharing.hsadminng.web.rest.errors;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class BadRequestAlertException extends AbstractThrowableProblem {

    private static final long serialVersionUID = 1L;

    private final String param;

    private final String errorKey;

    public BadRequestAlertException(String defaultMessage, String param, String errorKey) {
        this(ErrorConstants.DEFAULT_TYPE, defaultMessage, param, errorKey);
    }

    public BadRequestAlertException(URI type, String defaultMessage, String param, String errorKey) {
        super(type, defaultMessage, Status.BAD_REQUEST, null, null, null, getAlertParameters(param, errorKey));
        this.param = param;
        this.errorKey = errorKey;
    }

    public String getParam() {
        return param;
    }

    public String getErrorKey() {
        return errorKey;
    }

    private static Map<String, Object> getAlertParameters(String param, String errorKey) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("message", "error." + errorKey);
        parameters.put("params", param);
        return parameters;
    }
}
