package org.pentaho.proxy.creators.securitycontext;

import org.pentaho.proxy.creators.ProxyUtils;
import org.pentaho.platform.proxy.api.IProxyCreator;
import org.pentaho.platform.proxy.api.IProxyFactory;
import org.pentaho.platform.proxy.impl.ProxyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class S4SecurityContextProxyCreator implements IProxyCreator<SecurityContext> {

  private Logger logger = LoggerFactory.getLogger( getClass() );

  @Override public boolean supports( Class aClass ) {
    // supports legacy spring.security 2.0.8 SecurityContext
    return ProxyUtils.isRecursivelySupported( "org.springframework.security.context.SecurityContext", aClass );
  }

  @Override public SecurityContext create( Object o ) {
    return new ProxySecurityContext( o );
  }

  protected IProxyFactory getProxyFactory() {
    return ProxyUtils.getInstance().getProxyFactory();
  }

  private class ProxySecurityContext implements SecurityContext {

    private Object target;

    private Method getAuthenticationMethod;
    private Method setAuthenticationMethod;

    public ProxySecurityContext( Object target ) {
      this.target = target;
    }

    @Override public Authentication getAuthentication() {

      try {

        if( getAuthenticationMethod == null ) {
          getAuthenticationMethod = ProxyUtils.findMethodByName( target.getClass(), "getAuthentication" );
        }

        Object retVal = getAuthenticationMethod.invoke( target );

        if ( retVal != null ){
          return getProxyFactory().createProxy( retVal );
        }

      } catch ( InvocationTargetException | IllegalAccessException | ProxyException e ) {
        logger.error( e.getMessage() , e );
      }

      return null;
    }

    @Override public void setAuthentication( Authentication authentication ) {

      try {

        if( authentication == null ) {

          setAuthenticationMethod = ProxyUtils.findMethodByName( target.getClass(), "setAuthentication" );
          setAuthenticationMethod.invoke( target, null );

        } else {

          Object auth = ProxyUtils.getInstance().getProxyFactory().createProxy( authentication );

          setAuthenticationMethod = ProxyUtils.findMethodByName( target.getClass(), "setAuthentication", auth.getClass() );
          setAuthenticationMethod.invoke( target, auth );

        }

      } catch ( InvocationTargetException | IllegalAccessException | ProxyException e ) {
        logger.error( e.getMessage() , e );
      }
    }
  }

}
