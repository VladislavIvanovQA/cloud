package org.cloud.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFiles {
    private Integer id;
    private Integer userId;
    private Long size;
    private String fileName;
}
