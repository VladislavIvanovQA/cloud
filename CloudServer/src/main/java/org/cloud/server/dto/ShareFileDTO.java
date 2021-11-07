package org.cloud.server.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ShareFileDTO {
    private Integer id;
    private String username;
    private Integer fileId;
    private Integer limitDownload;
    private String link;
    private LocalDate expireDate;
}
