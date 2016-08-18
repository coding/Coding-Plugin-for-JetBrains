/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.coding.git.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.net.IdeHttpClientHelpers;
import com.intellij.util.net.ssl.CertificateManager;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.coding.git.CodingNetOpenAPICodeMsg;
import org.coding.git.exceptions.*;
import org.coding.git.util.CodingNetSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.coding.git.util.CodingNetAuthData;
import org.coding.git.util.CodingNetUrlUtil;
import org.coding.git.util.CodingNetUtil;

import javax.net.ssl.SSLHandshakeException;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.List;

import static org.coding.git.api.CodingNetApiUtil.createDataFromRaw;
import static org.coding.git.api.CodingNetApiUtil.fromJson;

/**
 * 封装了HttpClient的网络连接器
 *
 * @author robin
 */
public class CodingNetConnection {
    private static final Logger LOG = CodingNetUtil.LOG;

    private static final HttpRequestInterceptor PREEMPTIVE_BASIC_AUTH = new PreemptiveBasicAuthInterceptor();

    @NotNull
    private final String myHost;
    @NotNull
    private final CloseableHttpClient myClient;
    private final boolean myReusable;

    private volatile HttpUriRequest myRequest;
    private volatile boolean myAborted;

    @TestOnly
    public CodingNetConnection(@NotNull CodingNetAuthData auth) {
        this(auth, false);
    }

    public CodingNetConnection(@NotNull CodingNetAuthData auth, boolean reusable) {
        myHost = auth.getHost();
        myClient = createClient(auth);
        myReusable = reusable;
    }

    public enum HttpVerb {
        GET, POST, DELETE, HEAD, PATCH
    }

    @Nullable
    public JsonElement getRequest(@NotNull String path,
                                  @NotNull Header... headers) throws IOException {
        return request(path, null, Arrays.asList(headers), HttpVerb.GET).getJsonElement();
    }

    @Nullable
    public JsonElement postRequest(@NotNull String path,
                                   @Nullable String requestBody,
                                   @NotNull Header... headers) throws IOException {
        return request(path, requestBody, Arrays.asList(headers), HttpVerb.POST).getJsonElement();
    }

    public ResponsePage doPostRequest(@NotNull String path,
                                      @Nullable String requestBody,
                                      @NotNull Header... headers) throws IOException {
        return request(path, requestBody, Arrays.asList(headers), HttpVerb.POST);
    }


    @Nullable
    public JsonElement patchRequest(@NotNull String path,
                                    @Nullable String requestBody,
                                    @NotNull Header... headers) throws IOException {
        return request(path, requestBody, Arrays.asList(headers), HttpVerb.PATCH).getJsonElement();
    }

    @Nullable
    public JsonElement deleteRequest(@NotNull String path,
                                     @NotNull Header... headers) throws IOException {
        return request(path, null, Arrays.asList(headers), HttpVerb.DELETE).getJsonElement();
    }

    @NotNull
    public Header[] headRequest(@NotNull String path,
                                @NotNull Header... headers) throws IOException {
        return request(path, null, Arrays.asList(headers), HttpVerb.HEAD).getHeaders();
    }

    @NotNull
    public String getHost() {
        return myHost;
    }

    public void abort() {
        if (myAborted) return;
        myAborted = true;

        HttpUriRequest request = myRequest;
        if (request != null) request.abort();
    }

    public void close() throws IOException {
        myClient.close();
    }

    @NotNull
    private static CloseableHttpClient createClient(@NotNull CodingNetAuthData auth) {
        HttpClientBuilder builder = HttpClients.custom();

        return builder
                .setDefaultRequestConfig(createRequestConfig(auth))
                .setDefaultConnectionConfig(createConnectionConfig(auth))
                .setDefaultCredentialsProvider(createCredentialsProvider(auth))
                .setDefaultHeaders(createHeaders(auth))
                .addInterceptorFirst(PREEMPTIVE_BASIC_AUTH)
                .setSslcontext(CertificateManager.getInstance().getSslContext())
                //--加入cookie
                .setDefaultCookieStore(createCookieStore(auth))
                .setHostnameVerifier((X509HostnameVerifier) CertificateManager.HOSTNAME_VERIFIER)
                .build();
    }


    private static CookieStore createCookieStore(CodingNetAuthData auth) {
        BasicCookieStore cookieStore = new BasicCookieStore();
        CodingNetAuthData.BasicAuth basicAuth = auth.getBasicAuth();
        if (basicAuth != null) {
            String code = basicAuth.getSid();
            if (code != null) {
                BasicClientCookie cookie = new BasicClientCookie("sid", code);
                cookie.setDomain(".coding.net");
                cookie.setPath("/");
                cookieStore.addCookie(cookie);
            }
        }
        return cookieStore;
    }


    @NotNull
    private static RequestConfig createRequestConfig(@NotNull CodingNetAuthData auth) {
        RequestConfig.Builder builder = RequestConfig.custom();

        int timeout = CodingNetSettings.getInstance().getConnectionTimeout();
        builder
                .setConnectTimeout(timeout)
                .setSocketTimeout(timeout);

        if (auth.isUseProxy()) {
            IdeHttpClientHelpers.ApacheHttpClient4.setProxyForUrlIfEnabled(builder, auth.getHost());
        }

        return builder.build();
    }

    @NotNull
    private static ConnectionConfig createConnectionConfig(@NotNull CodingNetAuthData auth) {
        return ConnectionConfig.custom()
                .setCharset(Consts.UTF_8)
                .build();
    }


    @NotNull
    private static CredentialsProvider createCredentialsProvider(@NotNull CodingNetAuthData auth) {
        CredentialsProvider provider = new BasicCredentialsProvider();
        // Basic authentication
        CodingNetAuthData.BasicAuth basicAuth = auth.getBasicAuth();
        if (basicAuth != null) {
            provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(basicAuth.getLogin(), basicAuth.getPassword()));
        }

        if (auth.isUseProxy()) {
            IdeHttpClientHelpers.ApacheHttpClient4.setProxyCredentialsForUrlIfEnabled(provider, auth.getHost());
        }

        return provider;
    }

    @NotNull
    private static Collection<? extends Header> createHeaders(@NotNull CodingNetAuthData auth) {
        List<Header> headers = new ArrayList<Header>();
        CodingNetAuthData.TokenAuth tokenAuth = auth.getTokenAuth();
        if (tokenAuth != null) {
            headers.add(new BasicHeader("Authorization", "token " + tokenAuth.getToken()));
        }
        CodingNetAuthData.BasicAuth basicAuth = auth.getBasicAuth();
        if (basicAuth != null && basicAuth.getSid() != null) {
            headers.add(new BasicHeader("X-GitHub-OTP", basicAuth.getSid()));
        }
        return headers;
    }

    @NotNull
    private static String getRequestUrl(@NotNull String host, @NotNull String path) {
        return CodingNetUrlUtil.getApiUrl(host) + path;
    }

    @NotNull
    private ResponsePage request(@NotNull String path,
                                 @Nullable String requestBody,
                                 @NotNull Collection<Header> headers,
                                 @NotNull HttpVerb verb) throws IOException {
        return doRequest(getRequestUrl(myHost, path), requestBody, headers, verb);
    }

    @NotNull
    private ResponsePage doRequest(@NotNull String uri,
                                   @Nullable String requestBody,
                                   @NotNull Collection<Header> headers,
                                   @NotNull HttpVerb verb) throws IOException {
        if (myAborted) throw new CodingNetOperationCanceledException();

        if (EventQueue.isDispatchThread() && !ApplicationManager.getApplication().isUnitTestMode()) {
            LOG.warn("Network operation in EDT"); // TODO: fix
        }

        CloseableHttpResponse response = null;
        try {
            response = doREST(uri, requestBody, headers, verb);

            if (myAborted) throw new CodingNetOperationCanceledException();

            //--守护应用层HTTP请求返回状态码
            checkStatusCode(response, requestBody);

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return createResponse(response);
            }

            JsonElement ret = parseResponse(entity.getContent());
            if (ret.isJsonNull()) {
                return createResponse(response);
            }

            //--守护CodingNet返回的业务状态码
            checkCodingNetCode(ret);

            String nextPage = null;
            Header pageHeader = response.getFirstHeader("Link");
            if (pageHeader != null) {
                for (HeaderElement element : pageHeader.getElements()) {
                    NameValuePair rel = element.getParameterByName("rel");
                    if (rel != null && "next".equals(rel.getValue())) {
                        String urlString = element.toString();
                        int begin = urlString.indexOf('<');
                        int end = urlString.lastIndexOf('>');
                        if (begin == -1 || end == -1) {
                            LOG.error("Invalid 'Link' header", "{" + pageHeader.toString() + "}");
                            break;
                        }

                        nextPage = urlString.substring(begin + 1, end);
                        break;
                    }
                }
            }

            return createResponse(ret, nextPage, response);
        } catch (SSLHandshakeException e) { // User canceled operation from CertificateManager
            if (e.getCause() instanceof CertificateException) {
                LOG.info("Host SSL certificate is not trusted", e);
                throw new CodingNetOperationCanceledException("Host SSL certificate is not trusted", e);
            }
            throw e;
        } catch (IOException e) {
            if (myAborted) throw new CodingNetOperationCanceledException("Operation canceled", e);
            throw e;
        } finally {
            myRequest = null;
            if (response != null) {
                response.close();
            }
            if (!myReusable) {
                myClient.close();
            }
        }
    }

    /**
     * 守护OpenAPI业务码
     *
     * @param jsonElement
     */
    private void checkCodingNetCode(JsonElement jsonElement) throws IOException {
        int code = jsonElement.getAsJsonObject().get("code").getAsInt();
        if (code == CodingNetOpenAPICodeMsg.NO_LOGIN.getCode()) {
            CodingNetOpenAPICodeMsg.ICodingNetOpenAPICodeMsg codingNetOpenAPICodeMsg = (CodingNetOpenAPICodeMsg.ICodingNetOpenAPICodeMsg) fromJson(jsonElement, CodingNetOpenAPICodeMsg.NO_LOGIN.getClazz());
            throw new CodingNetAuthenticationException(codingNetOpenAPICodeMsg.getMessage());
        } else if (code == CodingNetOpenAPICodeMsg.NO_EXIST_USER.getCode()) {
            CodingNetOpenAPICodeMsg.ICodingNetOpenAPICodeMsg codingNetOpenAPICodeMsg = (CodingNetOpenAPICodeMsg.ICodingNetOpenAPICodeMsg) fromJson(jsonElement, CodingNetOpenAPICodeMsg.NO_EXIST_USER.getClazz());
            throw new CodingNetAuthenticationException(codingNetOpenAPICodeMsg.getMessage());
        } else if (code == CodingNetOpenAPICodeMsg.NEED_VERIFICATION_CODE.getCode()) {
            CodingNetOpenAPICodeMsg.ICodingNetOpenAPICodeMsg codingNetOpenAPICodeMsg = (CodingNetOpenAPICodeMsg.ICodingNetOpenAPICodeMsg) fromJson(jsonElement, CodingNetOpenAPICodeMsg.NEED_VERIFICATION_CODE.getClazz());
            throw new CodingNetAuthenticationException(codingNetOpenAPICodeMsg.getMessage());
        } else if (code == CodingNetOpenAPICodeMsg.USER_LOCKED.getCode()) {
            CodingNetOpenAPICodeMsg.ICodingNetOpenAPICodeMsg codingNetOpenAPICodeMsg = (CodingNetOpenAPICodeMsg.ICodingNetOpenAPICodeMsg) fromJson(jsonElement, CodingNetOpenAPICodeMsg.USER_LOCKED.getClazz());
            throw new CodingNetAuthenticationException(codingNetOpenAPICodeMsg.getMessage());
        } else if (code == CodingNetOpenAPICodeMsg.USER_PASSWORD_NO_CORRECT.getCode()) {
            CodingNetOpenAPICodeMsg.ICodingNetOpenAPICodeMsg codingNetOpenAPICodeMsg = (CodingNetOpenAPICodeMsg.ICodingNetOpenAPICodeMsg) fromJson(jsonElement, CodingNetOpenAPICodeMsg.USER_PASSWORD_NO_CORRECT.getClazz());
            throw new CodingNetAuthenticationException(codingNetOpenAPICodeMsg.getMessage());
        } else if (code == CodingNetOpenAPICodeMsg.AUTH_ERROR.getCode()) {
            CodingNetOpenAPICodeMsg.ICodingNetOpenAPICodeMsg codingNetOpenAPICodeMsg = (CodingNetOpenAPICodeMsg.ICodingNetOpenAPICodeMsg) fromJson(jsonElement, CodingNetOpenAPICodeMsg.AUTH_ERROR.getClazz());
            throw new CodingNetTwoFactorAuthenticationException(codingNetOpenAPICodeMsg.getMessage());
        } else if (code == CodingNetOpenAPICodeMsg.NEED_TWO_FACTOR_AUTH_CODE.getCode() || code == CodingNetOpenAPICodeMsg.TWO_FACTOR_AUTH_CODE_REQUIRED.getCode()) {
            return;
        } else if (code == CodingNetOpenAPICodeMsg.LOGIN_EXPIRED.getCode()) {
            CodingNetOpenAPICodeMsg.ICodingNetOpenAPICodeMsg codingNetOpenAPICodeMsg = (CodingNetOpenAPICodeMsg.ICodingNetOpenAPICodeMsg) fromJson(jsonElement, CodingNetOpenAPICodeMsg.LOGIN_EXPIRED.getClazz());
            throw new CodingNetTwoFactorAuthenticationException(codingNetOpenAPICodeMsg.getMessage());
        } else if (code != 0) {
            throw new CodingNetAuthenticationException();
        }
    }

    @NotNull
    private CloseableHttpResponse doREST(@NotNull final String uri,
                                         @Nullable final String requestBody,
                                         @NotNull final Collection<Header> headers,
                                         @NotNull final HttpVerb verb) throws IOException {
        HttpRequestBase request;
        switch (verb) {
            case POST:
                request = new HttpPost(uri);
                if (requestBody != null) {
                    ((HttpPost) request).setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
                }
                break;
            case PATCH:
                request = new HttpPatch(uri);
                if (requestBody != null) {
                    ((HttpPatch) request).setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
                }
                break;
            case GET:
                request = new HttpGet(uri);
                break;
            case DELETE:
                request = new HttpDelete(uri);
                break;
            case HEAD:
                request = new HttpHead(uri);
                break;
            default:
                throw new IllegalStateException("Unknown HttpVerb: " + verb.toString());
        }

        for (Header header : headers) {
            request.addHeader(header);
        }

        myRequest = request;
        return myClient.execute(request);
    }

    private static void checkStatusCode(@NotNull CloseableHttpResponse response, @Nullable String body) throws IOException {
        int code = response.getStatusLine().getStatusCode();
        switch (code) {
            case HttpStatus.SC_OK:
            case HttpStatus.SC_CREATED:
            case HttpStatus.SC_ACCEPTED:
            case HttpStatus.SC_NO_CONTENT:
                return;
            case HttpStatus.SC_UNAUTHORIZED:
            case HttpStatus.SC_PAYMENT_REQUIRED:
            case HttpStatus.SC_FORBIDDEN:
                //noinspection ThrowableResultOfMethodCallIgnored
                CodingNetStatusCodeException error = getStatusCodeException(response);

//        Header headerOTP = response.getFirstHeader("X-GitHub-OTP");
//        if (headerOTP != null) {
//          for (HeaderElement element : headerOTP.getElements()) {
//            if ("required".equals(element.getName())) {
//              throw new CodingNetTwoFactorAuthenticationException(error.getMessage());
//            }
//          }
//        }
//
//        if (error.getError() != null && error.getError().containsReasonMessage("API rate limit exceeded")) {
//          throw new GithubRateLimitExceededException(error.getMessage());
//        }

                throw new CodingNetAuthenticationException("Request response: " + error.getMessage());
            case HttpStatus.SC_BAD_REQUEST:
            case HttpStatus.SC_UNPROCESSABLE_ENTITY:
                LOG.info("body message:" + body);
                throw getStatusCodeException(response);
            default:
                throw getStatusCodeException(response);
        }
    }

    @NotNull
    private static CodingNetStatusCodeException getStatusCodeException(@NotNull CloseableHttpResponse response) {
        StatusLine statusLine = response.getStatusLine();
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                CodingNetErrorMessage error = fromJson(parseResponse(entity.getContent()), CodingNetErrorMessage.class);
                String message = statusLine.getReasonPhrase() + " - " + error.getMessage();
                return new CodingNetStatusCodeException(message, error, statusLine.getStatusCode());
            }
        } catch (IOException e) {
            LOG.info(e);
        }
        return new CodingNetStatusCodeException(statusLine.getReasonPhrase(), statusLine.getStatusCode());
    }

    @NotNull
    private static JsonElement parseResponse(@NotNull InputStream codingResponse) throws IOException {
        Reader reader = new InputStreamReader(codingResponse, CharsetToolkit.UTF8_CHARSET);
        try {
            return new JsonParser().parse(reader);
        } catch (JsonParseException jse) {
            throw new CodingNetJsonException("Couldn't parse Coding response", jse);
        } finally {
            reader.close();
        }
    }

    public static class PagedRequest<T> {
        @NotNull
        private String myPath;
        @NotNull
        private final Collection<Header> myHeaders;
        @NotNull
        private final Class<T> myResult;
        @NotNull
        private final Class<? extends ICodingNetDataConstructor[]> myRawArray;

        private boolean myFirstRequest = true;
        @Nullable
        private String myNextPage;

        public PagedRequest(@NotNull String path,
                            @NotNull Class<T> result,
                            @NotNull Class<? extends ICodingNetDataConstructor[]> rawArray,
                            @NotNull Header... headers) {
            myPath = path;
            myResult = result;
            myRawArray = rawArray;
            myHeaders = Arrays.asList(headers);
        }

        @NotNull
        public List<T> next(@NotNull CodingNetConnection connection) throws IOException {
            String url;
            if (myFirstRequest) {
                url = getRequestUrl(connection.getHost(), myPath);
                myFirstRequest = false;
            } else {
                if (myNextPage == null) throw new NoSuchElementException();
                url = myNextPage;
                myNextPage = null;
            }

            ResponsePage response = connection.doRequest(url, null, myHeaders, HttpVerb.GET);

            if (response.getJsonElement() == null) {
                throw new CodingNetConfusingException("Empty response");
            }

            //--Coding返回Json对象不是JSONArray,守护返回
            if (response.getJsonElement().isJsonArray()) {
                throw new CodingNetJsonException("Wrong json type: expected JsonArray", new Exception(response.getJsonElement().toString()));
            }

            myNextPage = response.getNextPage();

            List<T> result = new ArrayList<T>();
            JsonElement jsonElement = response.getJsonElement().getAsJsonObject().get("data").getAsJsonObject().get("list");
            if (!jsonElement.isJsonArray()) {
                throw new CodingNetJsonException("Wrong json type: expected JsonArray", new Exception(response.getJsonElement().toString()));
            }
            for (ICodingNetDataConstructor raw : fromJson(jsonElement, myRawArray)) {
                result.add(createDataFromRaw(raw, myResult));
            }
            return result;
        }

        public boolean hasNext() {
            return myFirstRequest || myNextPage != null;
        }

        @NotNull
        public List<T> getAll(@NotNull CodingNetConnection connection) throws IOException {
            List<T> result = new ArrayList<T>();
            while (hasNext()) {
                result.addAll(next(connection));
            }
            return result;
        }
    }

    private ResponsePage createResponse(@NotNull CloseableHttpResponse response) throws CodingNetOperationCanceledException {
        if (myAborted) throw new CodingNetOperationCanceledException();

        return new ResponsePage(null, null, response.getAllHeaders());
    }

    private ResponsePage createResponse(@NotNull JsonElement ret, @Nullable String path, @NotNull CloseableHttpResponse response)
            throws CodingNetOperationCanceledException {
        if (myAborted) throw new CodingNetOperationCanceledException();

        return new ResponsePage(ret, path, response.getAllHeaders());
    }

    public static class ResponsePage {
        @Nullable
        private final JsonElement myResponse;
        @Nullable
        private final String myNextPage;
        @NotNull
        private final Header[] myHeaders;

        public ResponsePage(@Nullable JsonElement response, @Nullable String next, @NotNull Header[] headers) {
            myResponse = response;
            myNextPage = next;
            myHeaders = headers;
        }

        @Nullable
        public JsonElement getJsonElement() {
            return myResponse;
        }

        @Nullable
        public String getNextPage() {
            return myNextPage;
        }

        @NotNull
        public Header[] getHeaders() {
            return myHeaders;
        }
    }

    private static class PreemptiveBasicAuthInterceptor implements HttpRequestInterceptor {
        @Override
        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
            CredentialsProvider provider = (CredentialsProvider) context.getAttribute(HttpClientContext.CREDS_PROVIDER);
            Credentials credentials = provider.getCredentials(AuthScope.ANY);
            if (credentials != null) {
                request.addHeader(new BasicScheme(Consts.UTF_8).authenticate(credentials, request, context));
            }
        }
    }
}
