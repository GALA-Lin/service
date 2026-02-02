package com.unlimited.sports.globox.venue.adapter.dto.wefitos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wefitos解锁槽位请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WefitosUnlockRequest {

    /**
     * 要解锁的锁定记录ID数组
     */
    @JsonProperty("ids[]")
    private String[] ids;
}
