package io.genderapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenderErrorResponse implements GenderApiResult {

    private boolean status;
    private Integer errno;
    private String errmsg;

    public boolean isStatus() {
        return status;
    }

    public Integer getErrno() {
        return errno;
    }

    public String getErrmsg() {
        return errmsg;
    }
}
