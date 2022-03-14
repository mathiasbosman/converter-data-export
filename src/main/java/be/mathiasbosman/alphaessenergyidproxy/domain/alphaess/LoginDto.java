package be.mathiasbosman.alphaessenergyidproxy.domain.alphaess;

import be.mathiasbosman.alphaessenergyidproxy.config.AlphaessProperties.Credentials;

public record LoginDto(String username, String password) {

  public static LoginDto fromCredentials(Credentials creds) {
    return new LoginDto(creds.getUsername(), creds.getPassword());
  }
}
