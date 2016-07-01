<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:param name="site-path" select="site-path" />
<xsl:variable name="portlet-id" select="portlet/portlet-id" />
    
<xsl:template match="portlet">

	<xsl:variable name="device_class">
	<xsl:choose>
		<xsl:when test="string(display-on-small-device)='0'">hidden-xs</xsl:when>
		<xsl:when test="string(display-on-normal-device)='0'">hidden-sm</xsl:when>
		<xsl:when test="string(display-on-large-device)='0'">hidden-md</xsl:when>
		<xsl:when test="string(display-on-xlarge-device)='0'">hidden-lg</xsl:when>
		<xsl:otherwise></xsl:otherwise>
	</xsl:choose>
	</xsl:variable>

    <div class="portlet {$device_class}">
        <xsl:if test="not(string(display-portlet-title)='1')">
            <h3>
                <xsl:value-of disable-output-escaping="yes" select="portlet-name" />
            </h3>
        </xsl:if>
		<div>
         	       <xsl:apply-templates select="document-list-portlet/document" />
         	       <xsl:apply-templates select="document-portlet/document" />
		</div>
	</div>
</xsl:template>

<xsl:template match="document">
<xsl:if test="not(string(document-xml-content)='null')">
        <a href="{$site-path}?document_id={document-id}&#38;portlet_id={$portlet-id}" target="_blank">
          	<xsl:for-each select="descendant::*">
                <xsl:value-of select="document-title" />
           </xsl:for-each>  
        </a>
		<br />
	    <xsl:for-each select="descendant::*">
        	<xsl:value-of select="document-summary" />
        </xsl:for-each>
</xsl:if>
</xsl:template>

</xsl:stylesheet>
