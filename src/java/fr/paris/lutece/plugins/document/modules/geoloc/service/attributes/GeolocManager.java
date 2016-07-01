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
package fr.paris.lutece.plugins.document.modules.geoloc.service.attributes;

import fr.paris.lutece.plugins.document.business.Document;
import fr.paris.lutece.plugins.document.business.attributes.AttributeTypeParameter;
import fr.paris.lutece.plugins.document.business.attributes.DocumentAttribute;
import fr.paris.lutece.plugins.document.business.portlet.DocumentPortlet;
import fr.paris.lutece.plugins.document.service.attributes.DefaultManager;
import fr.paris.lutece.plugins.leaflet.business.GeolocItem;
import fr.paris.lutece.plugins.leaflet.service.IconService;
import fr.paris.lutece.portal.business.portlet.PortletHome;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.util.AppLogService;

import org.apache.commons.lang.StringUtils;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Manager for Text Attribute
 */
public class GeolocManager extends DefaultManager
{
    private static final String MARK_STYLES = "styles";
    private static final String MARK_ICONS = "icons";
    private static final String MARK_EDIT_TYPES = "edit_types";
    private static final String TEMPLATE_CREATE_ATTRIBUTE = "admin/plugins/document/modules/geoloc/attributes/create_geoloc_gismap.html";
    private static final String TEMPLATE_MODIFY_ATTRIBUTE = "admin/plugins/document/modules/geoloc/attributes/modify_geoloc_gismap.html";
    private static final String TEMPLATE_CREATE_PARAMETERS_ATTRIBUTE = "admin/plugins/document/modules/geoloc/attributes/create_parameters_geoloc.html";
    private static final String TEMPLATE_MODIFY_PARAMETERS_ATTRIBUTE = "admin/plugins/document/modules/geoloc/attributes/modify_parameters_geoloc.html";
    private static final String KEY_ERROR_INVALID_GEOLOC_VALUE = "module.document.geoloc.attributeType.geoloc.error.invalidvalue";
    private static final String KEY_ERROR_MISSING_GEOLOC_VALUE = "module.document.geoloc.attributeType.geoloc.error.missingvalue";
    private static final String KEY_ERROR_NONARRAY_GEOLOC_VALUE = "module.document.geoloc.attributeType.geoloc.error.nonarrayvalue";
    private static final String KEY_ERROR_SHORTARRAY_GEOLOC_VALUE = "module.document.geoloc.attributeType.geoloc.error.shortarrayvalue";
    private static final String KEY_ERROR_NONNUMBER_GEOLOC_VALUE = "module.document.geoloc.attributeType.geoloc.error.nonnumbervalue";
    private static final String GEOLOC_JSON_PATH_GEOMETRY = "geometry";
    private static final String GEOLOC_JSON_PATH_GEOMETRY_COORDINATES = "coordinates";
    private static final String GEOLOC_JSON_PATH_FEATURESS = "features";
    private static final String TAG_GEOLOC_RESOURCE = "geoloc-resource";

    /**
     * Gets the template to enter the attribute value
     * @return The template to enter the attribute value
     */
    protected String getCreateTemplate(  )
    {
        return TEMPLATE_CREATE_ATTRIBUTE;
    }

    /**
     * Gets the template to modify the attribute value
     * @return The template to modify the attribute value
     */
    protected String getModifyTemplate(  )
    {
        return TEMPLATE_MODIFY_ATTRIBUTE;
    }

    /**
     * Gets the template to enter the parameters of the attribute value
     * @return The template to enter the parameters of the attribute value
     */
    protected String getCreateParametersTemplate(  )
    {
        return TEMPLATE_CREATE_PARAMETERS_ATTRIBUTE;
    }

    /**
     * Gets the template to modify the parameters of the attribute value
     * @return The template to modify the parameters of the attribute value
     */
    protected String getModifyParametersTemplate(  )
    {
        return TEMPLATE_MODIFY_PARAMETERS_ATTRIBUTE;
    }

    /**
     * {@inheritDoc}
     */
    public String getCreateParametersFormHtml( List<AttributeTypeParameter> listParameters, Locale locale )
    {
        List<String> editTypes = new ArrayList<String>(  );
        editTypes.add( "Address" );
        editTypes.add( "Point" );
        editTypes.add( "Line" );
        editTypes.add( "Polygon" );

        Map<String, Object> model = new HashMap<String, Object>(  );
        model.put( MARK_STYLES, PortletHome.getStylesList( DocumentPortlet.RESOURCE_ID ) );
        model.put( MARK_ICONS, IconService.getList(  ) );
        model.put( MARK_EDIT_TYPES, editTypes );

        return super.getCreateParametersFormHtml( listParameters, locale, model );
    }

    /**
     * {@inheritDoc}
     */
    public String getModifyParametersFormHtml( Locale locale, int nAttributeId )
    {
        List<String> editTypes = new ArrayList<String>(  );
        editTypes.add( "Address" );
        editTypes.add( "Point" );
        editTypes.add( "Line" );
        editTypes.add( "Polygon" );

        Map<String, Object> model = new HashMap<String, Object>(  );
        model.put( MARK_STYLES, PortletHome.getStylesList( DocumentPortlet.RESOURCE_ID ) );
        model.put( MARK_ICONS, IconService.getList(  ) );
        model.put( MARK_EDIT_TYPES, editTypes );

        return super.getModifyParametersFormHtml( locale, nAttributeId, model );
    }

    /**
     * {@inheritDoc}
     */
    public String validateValue( int nAttributeId, String strValue, Locale locale )
    {
        //The "required" check is done before, here we must not error if the string is empty or missing
        if ( ( strValue != null ) && !strValue.equals( "" ) )
        {
            JsonNode object;

            try
            {
                object = new ObjectMapper(  ).readTree( strValue );
            }
            catch ( IOException e )
            {
                return I18nService.getLocalizedString( KEY_ERROR_INVALID_GEOLOC_VALUE, locale );
            }

            JsonNode objCoordinates = object.path( GEOLOC_JSON_PATH_FEATURESS ).path( 0 ).path( GEOLOC_JSON_PATH_GEOMETRY )
                                            .path( GEOLOC_JSON_PATH_GEOMETRY_COORDINATES );

            if ( objCoordinates.isMissingNode(  ) )
            {
                return I18nService.getLocalizedString( KEY_ERROR_MISSING_GEOLOC_VALUE, locale );
            }
            else
            {
                if ( !objCoordinates.isArray(  ) )
                {
                    return I18nService.getLocalizedString( KEY_ERROR_NONARRAY_GEOLOC_VALUE, locale );
                }
                else
                {
                    Iterator<JsonNode> it = objCoordinates.getElements(  );

                    for ( int i = 0; i < 2; i++ )
                    {
                        if ( !it.hasNext(  ) )
                        {
                            return I18nService.getLocalizedString( KEY_ERROR_SHORTARRAY_GEOLOC_VALUE, locale );
                        }

                        JsonNode node = it.next(  );

                        if ( !node.isNumber(  ) )
                        {
                            return I18nService.getLocalizedString( KEY_ERROR_NONNUMBER_GEOLOC_VALUE, locale );
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Get the XML data corresponding to the attribute to build the document XML
     * content
     * @param document The document
     * @param attribute The attribute
     * @return The XML value of the attribute
     */
    public String getAttributeXmlValue( Document document, DocumentAttribute attribute )
    {
        if ( ( attribute.getTextValue(  ) != null ) && ( attribute.getTextValue(  ).length(  ) != 0 ) )
        {
            String strValue = attribute.getTextValue(  );

            JsonNode object = null;

            try
            {
                object = new ObjectMapper(  ).readTree( strValue );
            }
            catch ( IOException e )
            {
                AppLogService.error( "Erreur ", e );
            }

            JsonNode objCoordinates = object.path( GEOLOC_JSON_PATH_FEATURESS ).path( 0 );

            strValue = objCoordinates.toString(  );

            GeolocItem geolocItem;

            try
            {
                geolocItem = GeolocItem.fromJSON( strValue );
            }
            catch ( IOException e )
            {
                AppLogService.error( "Document Geoloc, error generating xml from JSON: " + strValue + ", exception" +
                    e );

                return StringUtils.EMPTY;
            }

            return geolocItem.toXML(  );
        }

        return StringUtils.EMPTY;
    }
}
