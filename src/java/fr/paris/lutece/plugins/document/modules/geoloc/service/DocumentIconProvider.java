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

import fr.paris.lutece.plugins.document.business.DocumentType;
import fr.paris.lutece.plugins.document.business.DocumentTypeHome;
import fr.paris.lutece.plugins.document.business.attributes.DocumentAttribute;
import fr.paris.lutece.plugins.document.business.attributes.DocumentAttributeHome;
import fr.paris.lutece.plugins.leaflet.service.IIconProvider;
import fr.paris.lutece.portal.service.util.AppLogService;

import java.util.List;


public class DocumentIconProvider implements IIconProvider
{
    //Attribute parameter pointing to an icon
    private static final String ATTR_PARAMETER_ICON = "icon";

    public String getIcon( String iconKey )
    {
        String documentTypeCode = iconKey.substring( 0, iconKey.indexOf( "-" ) );
        String documentAttributeCode = iconKey.substring( iconKey.indexOf( "-" ) + 1 );

        DocumentType type = DocumentTypeHome.findByPrimaryKey( documentTypeCode );
        DocumentAttribute attribute = null;

        for ( DocumentAttribute _attribute : type.getAttributes(  ) )
        {
            if ( _attribute.getCode(  ).equals( documentAttributeCode ) )
            {
                attribute = _attribute;

                break;
            }
        }

        if ( attribute == null )
        {
            AppLogService.error( "Document icon provider: no attribute " + documentAttributeCode + " for doctype " +
                documentTypeCode );

            return null;
        }

        List<String> parameterValues = DocumentAttributeHome.getAttributeParameterValues( attribute.getId(  ),
                ATTR_PARAMETER_ICON );

        if ( parameterValues.size(  ) > 0 )
        {
            return parameterValues.get( 0 );
        }

        return null;
    }
}
