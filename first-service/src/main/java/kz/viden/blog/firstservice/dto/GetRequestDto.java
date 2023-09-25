package kz.viden.blog.firstservice.dto;

public class GetRequestDto {

    private String httpClientType = "jetty";
    private String url;

    public GetRequestDto() {}

    public GetRequestDto(String httpClientType, String url) {
        this.httpClientType = httpClientType;
        this.url = url;
    }

    public String getHttpClientType() {
        return httpClientType;
    }

    public void setHttpClientType(String httpClientType) {
        this.httpClientType = httpClientType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
