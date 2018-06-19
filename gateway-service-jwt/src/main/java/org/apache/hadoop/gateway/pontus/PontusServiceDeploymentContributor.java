/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.gateway.pontus;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.hadoop.gateway.deploy.DeploymentContext;
import org.apache.hadoop.gateway.deploy.ServiceDeploymentContributorBase;
import org.apache.hadoop.gateway.descriptor.FilterDescriptor;
import org.apache.hadoop.gateway.descriptor.FilterParamDescriptor;
import org.apache.hadoop.gateway.descriptor.ResourceDescriptor;
import org.apache.hadoop.gateway.dispatch.GatewayDispatchFilter;
import org.apache.hadoop.gateway.filter.XForwardedHeaderFilter;
import org.apache.hadoop.gateway.filter.rewrite.api.UrlRewriteRulesDescriptor;
import org.apache.hadoop.gateway.filter.rewrite.api.UrlRewriteRulesDescriptorFactory;
import org.apache.hadoop.gateway.service.definition.*;
import org.apache.hadoop.gateway.topology.Provider;
import org.apache.hadoop.gateway.topology.Service;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.*;

public class PontusServiceDeploymentContributor extends ServiceDeploymentContributorBase {
  public static PrintStream out = System.out;

  static ServiceDefinition serviceDefinition;
  static UrlRewriteRulesDescriptor serviceRules;
  private static String SERVICE_FILE_NAME = "service";
  private static String REWRITE_FILE = "rewrite.xml";
  private static final String DISPATCH_ROLE = "dispatch";

  private static final String SERVICE_ROLE_PARAM = "serviceRole";

  private static final String XFORWARDED_FILTER_NAME = "XForwardedHeaderFilter";

  private static final String XFORWARDED_FILTER_ROLE = "xforwardedheaders";

  private static final String DEFAULT_HA_DISPATCH_CLASS = "org.apache.hadoop.gateway.ha.dispatch.DefaultHaDispatch";

  private static final String DISPATCH_IMPL_PARAM = "dispatch-impl";
  private static final String HTTP_CLIENT_FACTORY_PARAM = "httpClientFactory";



  private static Collection<File> getFileList(File servicesDir) {
    Collection<File> files;
    if (servicesDir.exists() && servicesDir.isDirectory()) {
      files = FileUtils.listFiles(servicesDir, new IOFileFilter() {
        @Override
        public boolean accept(File file) {
          return file.getName().contains(SERVICE_FILE_NAME);
        }

        @Override
        public boolean accept(File dir, String name) {
          return name.contains(SERVICE_FILE_NAME);
        }
      }, TrueFileFilter.INSTANCE);
    }
    else {
      return files = new HashSet<File>();
    }

    return files;
  }
  public static String getServicesLocation()
  {
    ProtectionDomain protDomain = PontusServiceDeploymentContributor.class.getProtectionDomain();

    URL location = protDomain.getCodeSource().getLocation();

    String locationStr = location.toExternalForm();
    String retVal = (locationStr.endsWith("jar") ? "jar:" : "") + locationStr
      + (locationStr.endsWith("jar") ? "!/services/" : locationStr.endsWith("/") ? "services/" : "/services/");

    return retVal;
  }

  static {
    try {

//      URL url =  PontusServiceDeploymentContributor.class.getClassLoader().getResource("services");
//      URL url = new URL(getServicesLocation());

//      File se rvicesDir = new File(url);

      InputStream inputStream = PontusServiceDeploymentContributor.class.getClassLoader().getResourceAsStream("services/pontus/0.0.1/service.xml");// new FileInputStream(servicesDir);

//      if (servicesDir.exists() && servicesDir.isDirectory()) {
//
//
        JAXBContext context = JAXBContext.newInstance(ServiceDefinition.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
//
//        for (File file : getFileList(servicesDir)) {

          serviceDefinition = (ServiceDefinition) unmarshaller.unmarshal(inputStream);
          inputStream.close();
          //look for rewrite rules as a sibling (for now)
//          serviceRules = ServiceDefinitionsLoader.loadRewriteRules(file.getParentFile());
        inputStream = PontusServiceDeploymentContributor.class.getClassLoader().getResourceAsStream("services/pontus/0.0.1/rewrite.xml");
         Reader reader = new InputStreamReader(inputStream);
      serviceRules = UrlRewriteRulesDescriptorFactory.load(
           "xml", reader);

    } catch (Throwable e) {
      e.printStackTrace();
    }

  }


  public PontusServiceDeploymentContributor() {
//    super(serviceDefinition, serviceRules);


  }

  @Override
  public String getRole() {
    return "Pontus";
  }

  @Override
  public String getName() {
    return "pontus";
  }

  private void contributeRewriteRules(DeploymentContext context, Service service) {
    if ( serviceRules != null ) {
      UrlRewriteRulesDescriptor clusterRules = context.getDescriptor("rewrite");
      clusterRules.addRules(serviceRules);
    }
  }

  @Override
  public void contributeService(DeploymentContext context, Service service) throws URISyntaxException {
    ResourceDescriptor resource = context.getGatewayDescriptor().addResource();
    resource.role(service.getRole());
    resource.pattern("/pontus/**");

    if (topologyContainsProviderType(context, "pontus_jwt")) {
      context.contributeFilter(service, resource, "pontus_jwt", "process", null);
    }
    try {
      contributeRewriteRules(context, service);
      contributeResources(context, service);
    } catch (Exception e) {

    }
  }

  private FilterDescriptor addDispatchFilterForClass(DeploymentContext context, Service service, ResourceDescriptor resource, String dispatchClass, String httpClientFactory) {
    FilterDescriptor filter = resource.addFilter().name(getName()).role(DISPATCH_ROLE).impl(GatewayDispatchFilter.class);
    filter.param().name(DISPATCH_IMPL_PARAM).value(dispatchClass);
    if (httpClientFactory != null) {
      filter.param().name(HTTP_CLIENT_FACTORY_PARAM).value(httpClientFactory);
    }
    for ( Map.Entry<String, String> serviceParam : service.getParams().entrySet() ) {
      filter.param().name(serviceParam.getKey()).value(serviceParam.getValue());
    }
    if ( context.getGatewayConfig().isHadoopKerberosSecured() ) {
      filter.param().name("kerberos").value("true");
    } else {
      //TODO: [sumit] Get rid of special case. Add config/param capabilities to service definitions?
      //special case for hive
      filter.param().name("basicAuthPreemptive").value("true");
    }
    return filter;
  }
  private void addDispatchFilter(DeploymentContext context, Service service, ResourceDescriptor resource, Route binding) {
    CustomDispatch customDispatch = binding.getDispatch();
    if ( customDispatch == null ) {
      customDispatch = serviceDefinition.getDispatch();
    }
    boolean isHaEnabled = isHaEnabled(context);
    if ( customDispatch != null ) {
      String haContributorName = customDispatch.getHaContributorName();
      String haClassName = customDispatch.getHaClassName();
      String httpClientFactory = customDispatch.getHttpClientFactory();
      if ( isHaEnabled) {
        if (haContributorName != null) {
          addDispatchFilter(context, service, resource, DISPATCH_ROLE, haContributorName);
        } else if (haClassName != null) {
          addDispatchFilterForClass(context, service, resource, haClassName, httpClientFactory);
        } else {
          addDefaultHaDispatchFilter(context, service, resource);
        }
      } else {
        String contributorName = customDispatch.getContributorName();
        if ( contributorName != null ) {
          addDispatchFilter(context, service, resource, DISPATCH_ROLE, contributorName);
        } else {
          String className = customDispatch.getClassName();
          if ( className != null ) {
            addDispatchFilterForClass(context, service, resource, className, httpClientFactory);
          } else {
            //final fallback to the default dispatch
            addDispatchFilter(context, service, resource, DISPATCH_ROLE, "http-client");
          }
        }
      }
    } else if (isHaEnabled) {
      addDefaultHaDispatchFilter(context, service, resource);
    } else {
      addDispatchFilter(context, service, resource, DISPATCH_ROLE, "http-client");
    }
  }

  private void addDefaultHaDispatchFilter(DeploymentContext context, Service service, ResourceDescriptor resource) {
    FilterDescriptor filter = addDispatchFilterForClass(context, service, resource, DEFAULT_HA_DISPATCH_CLASS, null);
    filter.param().name(SERVICE_ROLE_PARAM).value(service.getRole());
  }
  private boolean isHaEnabled(DeploymentContext context) {
    Provider provider = getProviderByRole(context, "ha");
    if ( provider != null && provider.isEnabled() ) {
      Map<String, String> params = provider.getParams();
      if ( params != null ) {
        if ( params.containsKey(getRole()) ) {
          return true;
        }
      }
    }
    return false;
  }

  private void contributeResources(DeploymentContext context, Service service) {
    Map<String, String> filterParams = new HashMap<String, String>();
    List<Route> bindings = serviceDefinition.getRoutes();
    for ( Route binding : bindings ) {
      List<Rewrite> filters = binding.getRewrites();
      if ( filters != null && !filters.isEmpty() ) {
        filterParams.clear();
        for ( Rewrite filter : filters ) {
          filterParams.put(filter.getTo(), filter.getApply());
        }
      }
      try {
        contributeResource(context, service, binding, filterParams);
      } catch ( URISyntaxException e ) {
        e.printStackTrace();
      }
    }

  }

  private void contributeResource(DeploymentContext context, Service service, Route binding, Map<String, String> filterParams) throws URISyntaxException {
    List<FilterParamDescriptor> params = new ArrayList<FilterParamDescriptor>();
    ResourceDescriptor resource = context.getGatewayDescriptor().addResource();
    resource.role(service.getRole());
    resource.pattern(binding.getPath());
    //add x-forwarded filter if enabled in config
    if (context.getGatewayConfig().isXForwardedEnabled()) {
      resource.addFilter().name(XFORWARDED_FILTER_NAME).role(XFORWARDED_FILTER_ROLE).impl(XForwardedHeaderFilter.class);
    }
    List<Policy> policyBindings = binding.getPolicies();
    if ( policyBindings == null ) {
      policyBindings = serviceDefinition.getPolicies();
    }
    if ( policyBindings == null ) {
      //add default set
      addDefaultPolicies(context, service, filterParams, params, resource);
    } else {
      addPolicies(context, service, filterParams, params, resource, policyBindings);
    }
    addDispatchFilter(context, service, resource, binding);
  }

  private void addPolicies(DeploymentContext context, Service service, Map<String, String> filterParams, List<FilterParamDescriptor> params, ResourceDescriptor resource, List<Policy> policyBindings) throws URISyntaxException {
    for ( Policy policyBinding : policyBindings ) {
      String role = policyBinding.getRole();
      if ( role == null ) {
        throw new IllegalArgumentException("Policy defined has no role for service " + service.getName());
      }
      role = role.trim().toLowerCase();
      if ( role.equals("rewrite") ) {
        addRewriteFilter(context, service, filterParams, params, resource);
      } else if ( topologyContainsProviderType(context, role) ) {
        context.contributeFilter(service, resource, role, policyBinding.getName(), null);
      }
    }
  }

  private void addDefaultPolicies(DeploymentContext context, Service service, Map<String, String> filterParams, List<FilterParamDescriptor> params, ResourceDescriptor resource) throws URISyntaxException {
    addWebAppSecFilters(context, service, resource);
    addAuthenticationFilter(context, service, resource);
    addRewriteFilter(context, service, filterParams, params, resource);
    addIdentityAssertionFilter(context, service, resource);
    addAuthorizationFilter(context, service, resource);
  }

  private void addRewriteFilter(DeploymentContext context, Service service, Map<String, String> filterParams, List<FilterParamDescriptor> params, ResourceDescriptor resource) throws URISyntaxException {
    if ( !filterParams.isEmpty() ) {
      for ( Map.Entry<String, String> filterParam : filterParams.entrySet() ) {
        params.add(resource.createFilterParam().name(filterParam.getKey()).value(filterParam.getValue()));
      }
    }
    addRewriteFilter(context, service, resource, params);
  }


}
