<?xml version="1.0" encoding="utf-8"?>
<!-- O primeiro Stylesheet com os dados encontrados através da pesquisa realizada no mongoDB
(ainda vai ser adicionada mais informação nos produtos, moeda e as informações adicionais ...)

EU ESTOU COMPLETAMENTE CONFUSO, nao percebo nada de como vamos fazer isto. Por exemplo, a pesquisa da loja
naquele mês devolve vários JSON com as várias linhas de venda, mas com várias, outras, coisas repetidas...
Não sei como se vai traduzir isso para o XSLT que vai fazer os XML... (NOTA: resolvido, ver a alteração da query!)

Este Stylesheet vai no fundo definir uma template definitiva para os XML, com base na xml String que vai ser criada
na API (com o uso da API 'org.json.XML').
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                version="1.0"
                xmlns:aud='ProjetoPEI/Grupo4/Entrega2/Auditoria'
                xmlns:loj="ProjetoPEI/Grupo4/EntregaFinal/Loja"
                xmlns:vnd="ProjetoPEI/Grupo4/Entrega2/Venda"
                xmlns:prd="ProjetoPEI/Grupo4/EntregaFinal/Produto">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>

    <xsl:template match="/">
        <!--
        Ainda nao sei como vamos adicionar o atributo da data de criação do documento...
        -->
        <xsl:element name="aud:Auditoria">
            <!--
            Atributos para a definição do 'schema instance' e os namespaces dos schemas para validação.
             -->
            <xsl:attribute name="xsi:schemaLocation">
                <!-- O XSLT guarda os enters (&#10;), portanto não se pode fazer enter na string abaixo ... -->
                <xsl:text disable-output-escaping="yes">ProjetoPEI/Grupo4/Entrega2/Auditoria ../SchemasDefinicaoModulos/SchemaAuditoriaLoja.xsd ProjetoPEI/Grupo4/EntregaFinal/Loja ../SchemasDefinicaoModulos/SchemaLoja.xsd ProjetoPEI/Grupo4/Entrega2/Venda ../SchemasDefinicaoModulos/SchemaVenda.xsd</xsl:text>
            </xsl:attribute>

            <xsl:element name="aud:Loja">
                <!-- com os dados disponibilizados pelos profs, so temos o id e nome da loja... o resto que está
                nos schemas que foi adicionado (a pedido dos mesmos) é completamente useless... [NOTA: eu ja corrigi
                 isto, basicamente algumas coisas ficaram com o minOccurs="0"... -->
                <xsl:attribute name="IDLoja">
                    <xsl:value-of select="/root/Store/text()"/>
                </xsl:attribute>
                <xsl:element name="loj:Nome">
                    <xsl:value-of select="/root/StoreName/text()"/>
                </xsl:element>
            </xsl:element>

            <xsl:element name="aud:Venda">
                <xsl:attribute name="dataInicio">
                    <xsl:value-of select="/root/OrderDate/text()"/>
                </xsl:attribute>

                <xsl:element name="aud:Moeda">
                    <xsl:attribute name="idTaxaCambio">
                        <xsl:variable name="checker" select="'NULL'"/>
                        <xsl:choose>
                            <xsl:when test="contains(/root/CurrencyRateID/text(),$checker)">
                                <xsl:text>0</xsl:text>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="/root/CurrencyRateID/text()"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <!-- Adicionar o value-of da moeda correspondente ao currencyRateID e a data (atributo) -->
                </xsl:element>

                <xsl:element name="aud:DadosVenda">
                    <xsl:attribute name="IDVenda">
                        <xsl:value-of select="/root/ReceiptID/text()"/>
                    </xsl:attribute>

                    <xsl:element name="vnd:InformacaoAdicionalVenda">
                        <!-- Adicionar as informações relativas ao schema das informações -->
                    </xsl:element>

                    <xsl:element name="vnd:SubTotal">
                        <xsl:value-of select="/root/SubTotal/text()"/>
                    </xsl:element>
                    <xsl:element name="vnd:TaxaImposto">
                        <xsl:value-of select="/root/TaxAmt/text()"/>
                    </xsl:element>
                    <xsl:element name="vnd:Cliente">
                        <xsl:attribute name="IDCliente">
                            <xsl:value-of select="/root/Customer/text()"/>
                        </xsl:attribute>
                    </xsl:element>

                    <xsl:element name="vnd:LinhasVenda">
                        <xsl:for-each select="/root/ReceiptLines">
                            <xsl:element name="vnd:DadosLinha">
                                <xsl:attribute name="IDLinha">
                                    <xsl:value-of select="ReceiptLineID/text()"/>
                                </xsl:attribute>
                                <!-- adicionar informaçao produto -->
                                <xsl:element name="vnd:Produto">
                                    <xsl:attribute name="IDProduto">
                                        <xsl:value-of select="ProductID/text()"/>
                                    </xsl:attribute>
                                </xsl:element>
                                <xsl:element name="vnd:PrecoPorUnidade">
                                    <xsl:value-of select="UnitPrice/text()"/>
                                </xsl:element>
                                <xsl:element name="vnd:Quantidade">
                                    <xsl:value-of select="Quantity/text()"/>
                                </xsl:element>
                                <xsl:element name="vnd:TotalLinha">
                                    <xsl:value-of select="LineTotal/text()"/>
                                </xsl:element>
                            </xsl:element>
                        </xsl:for-each>
                    </xsl:element>
                </xsl:element>
            </xsl:element>

        </xsl:element>
    </xsl:template>
</xsl:stylesheet>
