package io.genderapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenderSuccessResponse implements GenderApiResult {

    private boolean status;

    private String name;
    private String gender;
    private String country;

    @JsonProperty("total_names")
    private Integer totalNames;

    private Integer probability;
    private Integer used_credits;
    private Integer remaining_credits;
    private Long expires;
    private String q;
    private String duration;

    public boolean isStatus() {
        return status;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public String getCountry() {
        return country;
    }

    public Integer getTotalNames() {
        return totalNames;
    }

    public Integer getProbability() {
        return probability;
    }

    public Integer getUsed_credits() {
        return used_credits;
    }

    public Integer getRemaining_credits() {
        return remaining_credits;
    }

    public Long getExpires() {
        return expires;
    }

    public String getQ() {
        return q;
    }

    public String getDuration() {
        return duration;
    }
}
