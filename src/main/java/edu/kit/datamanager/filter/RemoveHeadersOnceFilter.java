/*
 * Copyright 2025 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.datamanager.filter;

import edu.kit.datamanager.configuration.SearchFilterConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Filter to remove duplicated headers from response.
 * <p>
 * The filter is used to remove duplicated headers from the response.
 * The filter is configured using a list of headers that should be deduplicated.
 * The filter is only applied to requests that match a given pattern.
 * Both headers and patterns are configured using the SearchConfiguration.
 *
 * @see SearchFilterConfiguration
 */
@Component
public class RemoveHeadersOnceFilter extends OncePerRequestFilter {
  /**
   * Search patterns.
   */
  private static Pattern[] searchPatterns = null;

  /**
   * Logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RemoveHeadersOnceFilter.class);

  /**
   * Search configuration.
   */
  @Autowired
  private SearchFilterConfiguration searchConfiguration;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    HttpServletResponseWrapper responseWrapper = new HttpServletResponseWrapper(response) {
      /**
       * Set of headers that already added.
       */
      private Set<String> dedupHeaders = new HashSet<>();

      @Override
      public void setHeader(String name, String value) {
        boolean ignoreHeader = false;
        String headerName = name.toLowerCase();
        LOGGER.trace("Setting header '{}' to value '{}'.", name, value);
        // Check if header should be ignored if mentioned more than once
        if (searchConfiguration.getHeadersLowerCase().contains(headerName)) {
          LOGGER.trace("Header '{}' is in deduplication list.", name);
          if (dedupHeaders.contains(headerName)) {
            LOGGER.trace("Header '{}' is already set --> ignore header.", name);
            ignoreHeader = true;
          } else {
            dedupHeaders.add(headerName);
          }
          if (!ignoreHeader) {
            super.setHeader(name, value);
          }
        }
      }

      @Override
      public void addHeader(String name, String value) {
        LOGGER.trace("Add header: '{}' with value '{}'.", name, value);
        setHeader(name, value);
      }
    };
    filterChain.doFilter(request, responseWrapper);

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Deduplication headers: '{}'", searchConfiguration.getHeadersLowerCase());
      for (String header : responseWrapper.getHeaderNames()) {
        LOGGER.trace("Header '{}': '{}'", header, responseWrapper.getHeader(header));
      }
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    boolean shouldNotFilter = true;
    if (searchConfiguration != null) {
      // if searchPatterns is not set, initialize the pattern array
      if (searchPatterns == null) {
        LOGGER.trace("Initialize search patterns.");
        searchPatterns = searchConfiguration.getSearchEndpointPatterns().stream().
                map(Pattern::compile).toArray(Pattern[]::new);
      }    // if request URI matches search pattern, use responseWrapper
      for (Pattern pattern : searchPatterns) {
        if (pattern.matcher(request.getRequestURI()).matches()) {
          LOGGER.trace("Request URI '{}' matches pattern '{}'.", request.getRequestURI(), pattern.pattern());
          shouldNotFilter = false;
          break;
        }
      }
    }
    return shouldNotFilter;
  }
}
