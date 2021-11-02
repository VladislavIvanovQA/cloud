package org.cloud.core.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"login", "password"})
public class User implements Serializable {
    private String login;
    private String password;
    private String username;

    public User(String login, String password) {
        this.login = login;
        this.password = password;
        this.username = null;
    }
}
