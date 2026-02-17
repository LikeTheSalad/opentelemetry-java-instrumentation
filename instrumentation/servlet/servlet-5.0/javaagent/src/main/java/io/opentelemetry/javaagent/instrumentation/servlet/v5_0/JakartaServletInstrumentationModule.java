/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.servlet.v5_0;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.internal.ExperimentalInstrumentationModule;
import io.opentelemetry.javaagent.instrumentation.servlet.common.async.AsyncContextInstrumentation;
import io.opentelemetry.javaagent.instrumentation.servlet.common.async.AsyncContextStartInstrumentation;
import io.opentelemetry.javaagent.instrumentation.servlet.common.async.AsyncStartInstrumentation;
import io.opentelemetry.javaagent.instrumentation.servlet.common.response.HttpServletResponseInstrumentation;
import io.opentelemetry.javaagent.instrumentation.servlet.common.service.ServletAndFilterInstrumentation;
import io.opentelemetry.javaagent.instrumentation.servlet.common.service.ServletOutputStreamInstrumentation;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AutoService(InstrumentationModule.class)
public class JakartaServletInstrumentationModule extends InstrumentationModule
    implements ExperimentalInstrumentationModule {
  private static final String BASE_PACKAGE = "jakarta.servlet";

  public JakartaServletInstrumentationModule() {
    super("servlet", "servlet-5.0");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Arrays.asList(
        new AsyncContextInstrumentation(
            BASE_PACKAGE, adviceClassName(".async.AsyncDispatchAdvice")),
        new AsyncContextStartInstrumentation(
            BASE_PACKAGE, adviceClassName(".async.AsyncContextStartAdvice")),
        new AsyncStartInstrumentation(BASE_PACKAGE, adviceClassName(".async.AsyncStartAdvice")),
        new ServletAndFilterInstrumentation(
            BASE_PACKAGE,
            adviceClassName(".service.JakartaServletServiceAdvice"),
            adviceClassName(".service.JakartaServletInitAdvice"),
            adviceClassName(".service.JakartaServletFilterInitAdvice")),
        new ServletOutputStreamInstrumentation(
            BASE_PACKAGE,
            adviceClassName(".Servlet5OutputStreamWriteBytesAndOffsetAdvice"),
            adviceClassName(".Servlet5OutputStreamWriteBytesAdvice"),
            adviceClassName(".Servlet5OutputStreamWriteIntAdvice")),
        new HttpServletResponseInstrumentation(
            BASE_PACKAGE, adviceClassName(".response.ResponseSendAdvice")));
  }

  private static String adviceClassName(String suffix) {
    return JakartaServletInstrumentationModule.class.getPackage().getName() + suffix;
  }

  // In OSGi environments (e.g. Payara/GlassFish), different servlet classes may be loaded by
  // different classloaders, resulting in multiple InstrumentationModuleClassLoaders for this module.
  // The VirtualField<Servlet, MappingResolver.Factory> stores a value in one classloader and reads
  // it from another. If MappingResolver.Factory is injected as a helper class, each classloader
  // gets its own copy, causing ClassCastException on retrieval.
  // By excluding these classes from helper injection, they are loaded from the shared agent
  // classloader instead, ensuring type compatibility across all InstrumentationModuleClassLoaders.
  private static final String[] CLASSES_TO_LOAD_FROM_AGENT =
      new String[] {
        "io.opentelemetry.instrumentation.servlet.internal.MappingResolver",
        "io.opentelemetry.instrumentation.servlet.internal.MappingResolver$Factory",
        "io.opentelemetry.instrumentation.servlet.internal.MappingResolver$WildcardMatcher",
        "io.opentelemetry.instrumentation.servlet.internal.MappingResolver$PrefixMatcher",
        "io.opentelemetry.instrumentation.servlet.internal.MappingResolver$SuffixMatcher",
        "io.opentelemetry.instrumentation.servlet.internal.ServletMappingResolverFactory",
        "io.opentelemetry.instrumentation.servlet.internal.ServletMappingResolverFactory$Mappings",
        "io.opentelemetry.instrumentation.servlet.internal.ServletMappingResolverFactory$MappingResolverHolder",
      };

  @Override
  public List<String> injectedClassNames() {
    return Stream.of(CLASSES_TO_LOAD_FROM_AGENT).collect(Collectors.toList());
  }

  @Override
  public boolean isIndyReady() {
    return true;
  }
}
