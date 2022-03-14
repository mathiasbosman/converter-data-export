package be.mathiasbosman.alphaessenergyidproxy.domain.alphaess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import be.mathiasbosman.alphaessenergyidproxy.config.AlphaessProperties;
import be.mathiasbosman.alphaessenergyidproxy.config.AlphaessProperties.Credentials;
import be.mathiasbosman.alphaessenergyidproxy.domain.alphaess.response.LoginResponseEntity;
import be.mathiasbosman.alphaessenergyidproxy.domain.alphaess.response.LoginResponseEntity.LoginData;
import be.mathiasbosman.alphaessenergyidproxy.domain.alphaess.response.SticsByPeriodResponseEntity;
import be.mathiasbosman.alphaessenergyidproxy.domain.alphaess.response.SticsByPeriodResponseEntity.Statistics;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class AlphaessServiceTest {

  @Mock
  private RestTemplate restTemplate;
  private final AlphaessProperties alphaessProperties = new AlphaessProperties();

  private AlphaessService alphaessService;

  @BeforeEach
  void initProps() throws MalformedURLException {
    alphaessProperties.setBaseUrl(new URL("https://foo"));
    alphaessService = new AlphaessService(restTemplate, alphaessProperties);
  }

  @Test
  void buildUri() throws URISyntaxException {
    assertThat(alphaessService.buildUri("bar")).isEqualTo(new URI("https://foo/bar"));
  }

  @Test
  void authentication() {
    LoginData mockedData = mockAuth();

    LoginData authenticate = alphaessService.authenticate(new LoginDto("foo", "bar"));

    assertThat(authenticate).isEqualTo(mockedData);
  }

  @Test
  void authenticationFails() {
    when(restTemplate.postForObject(any(), any(), eq(LoginResponseEntity.class)))
        .thenReturn(null);

    assertThrows(IllegalStateException.class,
        () -> alphaessService.authenticate(new LoginDto("foo", "bar")));
  }

  @Test
  void isTokenValid() {
    LoginData missingToken = LoginData.builder().accessToken(null).build();
    LoginData expiresInIsZero = LoginData.builder().accessToken("123").expiresIn(0).build();
    LoginData creationTimeIsNull = LoginData.builder().accessToken("123").expiresIn(10)
        .tokenCreateTime(null).build();
    LoginData validToken = LoginData.builder().accessToken("123").expiresIn(1000)
        .tokenCreateTime(new Date()).build();

    assertThat(alphaessService.isTokenValid(missingToken)).isFalse();
    assertThat(alphaessService.isTokenValid(expiresInIsZero)).isFalse();
    assertThat(alphaessService.isTokenValid(creationTimeIsNull)).isFalse();
    assertThat(alphaessService.isTokenValid(validToken)).isTrue();
  }

  @Test
  void getDailyStatics() {
    mockAuth();
    SticsByPeriodResponseEntity response = new SticsByPeriodResponseEntity();
    response.setData(Statistics.builder().feedIn(50).build());
    when(restTemplate.postForObject(any(), any(), eq(SticsByPeriodResponseEntity.class)))
        .thenReturn(response);

    Optional<Statistics> stats = alphaessService.getDailyStatistics("sn", new Date());
    assertThat(stats).isNotEmpty();
    assertThat(stats.get().getFeedIn()).isEqualTo(50);
  }

  @Test
  void getDailyStatisticsIsEmpty() {
    mockAuth();
    when(restTemplate.postForObject(any(), any(), eq(SticsByPeriodResponseEntity.class)))
        .thenReturn(null);

    assertThat(alphaessService.getDailyStatistics("123", new Date())).isEmpty();
  }

  private LoginData mockAuth() {
    alphaessProperties.setCredentials(Credentials.builder().username("foo").password("bar").build());
    LoginData loginData = LoginData.builder().accessToken("123").build();
    LoginResponseEntity responseEntity = new LoginResponseEntity();
    responseEntity.setData(loginData);

    when(restTemplate.postForObject(any(), any(), eq(LoginResponseEntity.class)))
        .thenReturn(responseEntity);

    return loginData;
  }

}