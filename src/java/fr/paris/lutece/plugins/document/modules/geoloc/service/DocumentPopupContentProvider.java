/*
 * Copyright (c) 2002-2014, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.document.modules.geoloc.service;

import fr.paris.lutece.plugins.document.business.Document;
import fr.paris.lutece.plugins.document.business.DocumentHome;
import fr.paris.lutece.plugins.document.business.DocumentType;
import fr.paris.lutece.plugins.document.business.DocumentTypeHome;
import fr.paris.lutece.plugins.document.business.attributes.DocumentAttribute;
import fr.paris.lutece.plugins.document.business.attributes.DocumentAttributeHome;
import fr.paris.lutece.plugins.document.business.portlet.DocumentListPortletHome;
import fr.paris.lutece.plugins.document.service.publishing.PublishingService;
import fr.paris.lutece.plugins.leaflet.rest.service.IPopupContentProvider;
import fr.paris.lutece.portal.business.XmlContent;
import fr.paris.lutece.portal.business.page.Page;
import fr.paris.lutece.portal.business.page.PageHome;
import fr.paris.lutece.portal.business.portlet.Portlet;
import fr.paris.lutece.portal.business.style.ModeHome;
import fr.paris.lutece.portal.business.style.StyleHome;
import fr.paris.lutece.portal.business.stylesheet.StyleSheet;
import fr.paris.lutece.portal.business.stylesheet.StyleSheetHome;
import fr.paris.lutece.portal.service.html.XmlTransformerService;
import fr.paris.lutece.portal.service.portal.PortalMenuService;
import fr.paris.lutece.portal.service.security.SecurityService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.web.constants.Parameters;
import fr.paris.lutece.portal.web.l10n.LocaleService;
import fr.paris.lutece.util.xml.XmlUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;


public class DocumentPopupContentProvider implements IPopupContentProvider
{
    //Attribute parameter pointing to a style
    private static final String ATTR_PARAMETER_STYLE = "style";

    //Xsl cache key
    private static final String DOCUMENT_STYLE_PREFIX_ID = "document-popup-";

    //To mimic a DocumentPortlet
    private static final String TAG_DOCUMENT_PORTLET = "document-portlet";
    private static final String VALUE_TRUE = "1";
    private static final String VALUE_FALSE = "0";

    // Parameters, copy from PageService.java
    private static final String MARKER_IS_USER_AUTHENTICATED = "is-user-authenticated";
    private static final String PARAMETER_SITE_PATH = "site-path";
    private static final String PARAMETER_USER_SELECTED_LOCALE = "user-selected-language";
    private static final String PARAMETER_PLUGIN_NAME = "plugin-name";
    private static final String PARAMETER_PORTLET = "portlet";

    public String getPopup( HttpServletRequest request, String strIdDocument, String strCode )
    {
        int nDocId;

        try
        {
            nDocId = Integer.parseInt( strIdDocument );
        }
        catch ( NumberFormatException nfe )
        {
            AppLogService.error( "Document popup rest API: invalid docId: " + strIdDocument + " exeception " + nfe );

            return null;
        }

        Document document = DocumentHome.findByPrimaryKeyWithoutBinaries( nDocId );

        if ( ( document == null ) || ( !document.isValid(  ) ) )
        {
            AppLogService.error( "Document popup rest API: invalid document " + strIdDocument );

            return null;
        }

        //Find first portlet of type DocumentList
        Collection<Portlet> portlets = PublishingService.getInstance(  ).getPortletsByDocumentId( strIdDocument );

        if ( portlets.size(  ) == 0 )
        {
            AppLogService.error( "Document popup rest API: no portlets for doc " + strIdDocument );

            return null;
        }

        Iterator<Portlet> iterator = portlets.iterator(  );

        //Find first portlet with correct settings
        Portlet portlet = null;

        do
        {
            Portlet p = iterator.next(  );

            if ( p.getStatus(  ) == Portlet.STATUS_UNPUBLISHED )
            {
                AppLogService.debug( "Document popup rest API: refuse unpublished portlet for " + strIdDocument +
                    ", portlet " + p.getId(  ) );

                continue;
            }

            if ( SecurityService.isAuthenticationEnable(  ) )
            {
                String strRolePortlet = p.getRole(  );

                if ( !strRolePortlet.equals( Page.ROLE_NONE ) )
                {
                    if ( !SecurityService.getInstance(  ).isUserInRole( request, strRolePortlet ) )
                    {
                        AppLogService.debug( "Document popup rest API: refuse portlet role for " + strIdDocument +
                            ", portlet " + p.getId(  ) + ", role " + strRolePortlet );

                        continue;
                    }
                }

                String strRolePage = PageHome.getPage( p.getPageId(  ) ).getRole(  );

                if ( !strRolePage.equals( Page.ROLE_NONE ) )
                {
                    if ( !SecurityService.getInstance(  ).isUserInRole( request, strRolePage ) )
                    {
                        AppLogService.debug( "Document popup rest API: refuse page role for " + strIdDocument +
                            ", portlet " + p.getId(  ) + ", role " + strRolePage );

                        continue;
                    }
                }
            }

            portlet = p;
        }
        while ( ( portlet == null ) && iterator.hasNext(  ) );

        if ( portlet == null )
        {
            AppLogService.error( "Document popup rest API: no matching DocumentList portlets for doc " + strIdDocument );

            return null;
        }

        DocumentType type = DocumentTypeHome.findByPrimaryKey( document.getCodeDocumentType(  ) );
        DocumentAttribute attribute = null;

        for ( DocumentAttribute _attribute : type.getAttributes(  ) )
        {
            if ( _attribute.getCode(  ).equals( strCode ) )
            {
                attribute = _attribute;

                break;
            }
        }

        if ( attribute == null )
        {
            AppLogService.error( "Document popup rest API: no attribute " + strCode + " for doc " + strIdDocument +
                " of type " + type.getCode(  ) );

            return null;
        }

        String strStyleId = DocumentAttributeHome.getAttributeParameterValues( attribute.getId(  ), ATTR_PARAMETER_STYLE )
                                                 .get( 0 );
        Collection<StyleSheet> listStyleSheet = StyleHome.getStyleSheetList( Integer.parseInt( strStyleId ) );

        if ( listStyleSheet.isEmpty(  ) )
        {
            AppLogService.error( "Document popup rest API: no stylesheet for style " + strStyleId );

            return null;
        }

        StyleSheet stylesheet = StyleSheetHome.findByPrimaryKey( listStyleSheet.iterator(  ).next(  ).getId(  ) );

        //Copy from Portlet addPortletTags
        StringBuffer strXml = new StringBuffer(  );
        XmlUtil.beginElement( strXml, XmlContent.TAG_PORTLET );
        XmlUtil.addElementHtml( strXml, XmlContent.TAG_PORTLET_NAME, portlet.getName(  ) );
        XmlUtil.addElement( strXml, XmlContent.TAG_PORTLET_ID, portlet.getId(  ) );
        XmlUtil.addElement( strXml, XmlContent.TAG_PAGE_ID, portlet.getPageId(  ) );
        XmlUtil.addElement( strXml, XmlContent.TAG_PLUGIN_NAME, portlet.getPluginName(  ) );
        XmlUtil.addElement( strXml, XmlContent.TAG_DISPLAY_PORTLET_TITLE, portlet.getDisplayPortletTitle(  ) );

        String strDisplayOnSmallDevice = ( ( portlet.getDeviceDisplayFlags(  ) & Portlet.FLAG_DISPLAY_ON_SMALL_DEVICE ) != 0 )
            ? VALUE_TRUE : VALUE_FALSE;
        XmlUtil.addElement( strXml, XmlContent.TAG_DISPLAY_ON_SMALL_DEVICE, strDisplayOnSmallDevice );

        String strDisplayOnNormalDevice = ( ( portlet.getDeviceDisplayFlags(  ) &
            Portlet.FLAG_DISPLAY_ON_NORMAL_DEVICE ) != 0 ) ? VALUE_TRUE : VALUE_FALSE;
        XmlUtil.addElement( strXml, XmlContent.TAG_DISPLAY_ON_NORMAL_DEVICE, strDisplayOnNormalDevice );

        String strDisplayOnLargeDevice = ( ( portlet.getDeviceDisplayFlags(  ) & Portlet.FLAG_DISPLAY_ON_LARGE_DEVICE ) != 0 )
            ? VALUE_TRUE : VALUE_FALSE;
        XmlUtil.addElement( strXml, XmlContent.TAG_DISPLAY_ON_LARGE_DEVICE, strDisplayOnLargeDevice );

        String strDisplayOnXLargeDevice = ( ( portlet.getDeviceDisplayFlags(  ) &
            Portlet.FLAG_DISPLAY_ON_XLARGE_DEVICE ) != 0 ) ? VALUE_TRUE : VALUE_FALSE;
        XmlUtil.addElement( strXml, XmlContent.TAG_DISPLAY_ON_XLARGE_DEVICE, strDisplayOnXLargeDevice );

        XmlUtil.beginElement( strXml, TAG_DOCUMENT_PORTLET );
        strXml.append( document.getXml( request, portlet.getId(  ) ) );
        XmlUtil.endElement( strXml, TAG_DOCUMENT_PORTLET );
        XmlUtil.endElement( strXml, XmlContent.TAG_PORTLET );

        XmlTransformerService xmlTransformerService = new XmlTransformerService(  );
        String strXslUniquePrefix = DOCUMENT_STYLE_PREFIX_ID + stylesheet.getId(  );

        //Copy from PageService getParams
        Map<String, String> mapModifyParam = new HashMap<String, String>(  );
        mapModifyParam.put( PARAMETER_USER_SELECTED_LOCALE,
            LocaleService.getUserSelectedLocale( request ).getLanguage(  ) );

        mapModifyParam.put( PARAMETER_SITE_PATH, AppPathService.getPortalUrl(  ) );

        if ( SecurityService.isAuthenticationEnable(  ) )
        {
            mapModifyParam.put( MARKER_IS_USER_AUTHENTICATED,
                ( SecurityService.getInstance(  ).getRegisteredUser( request ) != null ) ? VALUE_TRUE : VALUE_FALSE );
        }

        mapModifyParam.put( Parameters.PAGE_ID, Integer.toString( portlet.getPageId(  ) ) );

        Map<String, String> mapXslParams = portlet.getXslParams(  );
        Map<String, String> mapParams = mapModifyParam;

        if ( mapXslParams != null )
        {
            for ( Entry<String, String> entry : mapXslParams.entrySet(  ) )
            {
                mapParams.put( entry.getKey(  ), entry.getValue(  ) );
            }
        }

        String result = xmlTransformerService.transformBySourceWithXslCache( strXml.toString(  ),
                stylesheet.getSource(  ), strXslUniquePrefix, mapParams,
                ModeHome.getOuputXslProperties( PortalMenuService.MODE_NORMAL ) );

        return result;
    }
}
