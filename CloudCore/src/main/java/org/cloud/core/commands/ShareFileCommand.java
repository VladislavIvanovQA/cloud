package org.cloud.core.commands;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareFileCommand implements Serializable {
    private String fileName;
    private boolean singleDownload;
    private LocalDate expireDateTime;
}
