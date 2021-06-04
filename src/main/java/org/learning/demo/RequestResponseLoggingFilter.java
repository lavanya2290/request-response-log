package org.learning.demo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import lombok.Data;
import lombok.val;


/**
 * Web filter for request and reposne logging
 *
 * @see ContentCachingRequestWrapper
 * @see ContentCachingResponseWrapper
 */
public class RequestResponseLoggingFilter extends CommonsRequestLoggingFilter {

    private String beforeMessagePrefix = DEFAULT_BEFORE_MESSAGE_PREFIX;

    private String beforeMessageSuffix = DEFAULT_BEFORE_MESSAGE_SUFFIX;

    private String afterMessagePrefix = DEFAULT_AFTER_MESSAGE_PREFIX;

    private String afterMessageSuffix = DEFAULT_AFTER_MESSAGE_SUFFIX;

    private static final List<MediaType> VISIBLE_TYPES = Arrays.asList(
            MediaType.valueOf("text/*"),
            MediaType.APPLICATION_FORM_URLENCODED,
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML,
            MediaType.valueOf("application/*+json"),
            MediaType.valueOf("application/*+xml"),
            MediaType.MULTIPART_FORM_DATA
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        HttpServletRequest requestWrapper = request;
        HttpServletResponse responseWrapper = response;

        if (shouldLog(requestWrapper) && !isAsyncStarted(requestWrapper)) {
            if (isIncludePayload() && !isAsyncStarted(requestWrapper) && !(requestWrapper instanceof ContentCachingRequestWrapper)) {
                requestWrapper = wrapRequest(request);
                responseWrapper = wrapResponse(response);
            }
            beforeRequest(requestWrapper, createMessage(requestWrapper, beforeMessagePrefix, beforeMessageSuffix));
        }
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            if (shouldLog(requestWrapper) && !isAsyncStarted(requestWrapper)) {
                afterRequest(requestWrapper, createMessage(requestWrapper, beforeMessagePrefix, beforeMessageSuffix));
                logResponse(responseWrapper, afterMessagePrefix, afterMessageSuffix);
                ((ContentCachingResponseWrapper) responseWrapper).copyBodyToResponse();
            }
        }
    }

    @Override
    protected String createMessage(HttpServletRequest request, String prefix, String suffix) {
        StringBuilder msg = new StringBuilder();
        msg.append(prefix + " ");
        msg.append(request.getMethod() + " ");
        msg.append(request.getRequestURI());

        if (isIncludeQueryString()) {
            val queryString = request.getQueryString();
            if (queryString != null) {
                msg.append(queryString);
            }
        }
        msg.append(System.lineSeparator());


        if (isIncludeHeaders()) {
            Collections.list(request.getHeaderNames()).forEach(headerName ->
                    Collections.list(request.getHeaders(headerName)).forEach(headerValue -> {
                        msg.append(headerName + ": ");
                        msg.append(headerValue);
                        msg.append(System.lineSeparator());
                    }));
        }
        if (isIncludeClientInfo()) {
            String client = request.getRemoteAddr();
            if (StringUtils.hasLength(client)) {
                msg.append("client=").append(client);

            }
            HttpSession session = request.getSession(false);
            if (session != null) {
                msg.append(";session=").append(session.getId());
            }
            String user = request.getRemoteUser();
            if (user != null) {
                msg.append(";user=").append(user);
            }
            msg.append(System.lineSeparator());
        }
        if (isIncludePayload()) {
            String payload = getMessagePayload(request);
            if (payload != null) {
                msg.append("payload=").append(System.lineSeparator()).append(payload).append(System.lineSeparator());
            }
        }
        msg.append(suffix);
        return msg.toString();
    }

    @Override
    public void setBeforeMessagePrefix(String beforeMessagePrefix) {
        this.beforeMessagePrefix = beforeMessagePrefix;
    }

    @Override
    public void setBeforeMessageSuffix(String beforeMessageSuffix) {
        this.beforeMessageSuffix = beforeMessageSuffix;
    }

    @Override
    public void setAfterMessagePrefix(String afterMessagePrefix) {
        this.afterMessagePrefix = afterMessagePrefix;
    }

    @Override
    public void setAfterMessageSuffix(String afterMessageSuffix) {
        this.afterMessageSuffix = afterMessageSuffix;
    }

    private void logResponse(HttpServletResponse response, String prefix, String suffix) {
        StringBuilder stringBuilder = new StringBuilder();
        val status = response.getStatus();
        stringBuilder.append(prefix + " ");
        stringBuilder.append(status + " ");
        stringBuilder.append(HttpStatus.valueOf(status).getReasonPhrase());
        stringBuilder.append(System.lineSeparator());
        response.getHeaderNames().forEach(headerName ->
                response.getHeaders(headerName).forEach(headerValue -> {
                    stringBuilder.append(headerName);
                    stringBuilder.append(headerValue);
                    stringBuilder.append(System.lineSeparator());
                }));
        stringBuilder.append(getPayLoad(response)).append(System.lineSeparator());
        stringBuilder.append(suffix);
        logger.debug(stringBuilder.toString());
    }

    private String getPayLoad(HttpServletResponse response) {
        StringBuilder stringBuilder = new StringBuilder();
        val mediaType = MediaType.valueOf(response.getContentType());
        val visible = VISIBLE_TYPES.stream().anyMatch(visibleType -> visibleType.includes(mediaType));
        if (visible) {
            ContentCachingResponseWrapper wrapper =
                    WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
            if (wrapper != null) {
                byte[] content = wrapper.getContentAsByteArray();
                if (content.length > 0) {
                    int length = Math.min(content.length, getMaxPayloadLength());
                    try {
                        stringBuilder.append(new String(content, 0, length, wrapper.getCharacterEncoding()));
                    } catch (UnsupportedEncodingException ex) {
                    }
                }
            }
        }
        return stringBuilder.toString();
    }

    private ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper) {
            return (ContentCachingRequestWrapper) request;
        } else {
            return new ContentCachingRequestWrapper(request, getMaxPayloadLength());
        }
    }

    private ContentCachingResponseWrapper wrapResponse(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper) {
            return (ContentCachingResponseWrapper) response;
        } else {
            return new ContentCachingResponseWrapper(response);
        }
    }
}
