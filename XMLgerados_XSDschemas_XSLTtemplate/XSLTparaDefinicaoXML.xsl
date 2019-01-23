<?xml version="1.0" encoding="utf-8"?>
<!--
Este Stylesheet vai no fundo definir uma template definitiva para os XML, com base na xml String que vai ser criada
na API (com o uso da API 'org.json.XML').

Ainda vai ser adicionada mais informação nos produtos, moeda e as informações adicionais ...
(NOTA: isto já está resolvido, basicamente o que eu fiz foi juntar a informação dos 3 CSV na mesma XML String
podendo asseder-se ao conteúdo específico através do uso do elemento relativo (para usar os dados dos produtos -
"/DadosProdutos/<elemento>/text()" e das moedas "/DadosMoeda..." e a informação contida na venda "DadosVenda" -
isto só é possivel utilizando a <xsl:template> no elemento "/root", como se pode ver abaixo...)

EU ESTOU COMPLETAMENTE CONFUSO, nao percebo nada de como vamos fazer isto. Por exemplo, a pesquisa da loja
naquele mês devolve vários JSON com as várias linhas de venda, mas com várias, outras, coisas repetidas...
Não sei como se vai traduzir isso para o XSLT que vai fazer os XML... (NOTA: resolvido, ver a alteração da query!)
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:date="http://exslt.org/dates-and-times" version="1.0" exclude-result-prefixes="date"
                xmlns:aud='ProjetoPEI/Grupo4/Entrega2/Auditoria'
                xmlns:loj="ProjetoPEI/Grupo4/EntregaFinal/Loja"
                xmlns:vnd="ProjetoPEI/Grupo4/Entrega2/Venda"
                xmlns:prd="ProjetoPEI/Grupo4/EntregaFinal/Produto">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>

    <xsl:variable name="checker" select="'NULL'"/>
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
                <xsl:text disable-output-escaping="yes">ProjetoPEI/Grupo4/Entrega2/Auditoria ../SchemasDefinicaoModulos/SchemaAuditoriaLoja.xsd ProjetoPEI/Grupo4/EntregaFinal/Loja ../SchemasDefinicaoModulos/SchemaLoja.xsd ProjetoPEI/Grupo4/Entrega2/Venda ../SchemasDefinicaoModulos/SchemaVenda.xsd ProjetoPEI/Grupo4/EntregaFinal/Produto ../SchemasDefinicaoModulos/SchemaProduto.xsd</xsl:text>
            </xsl:attribute>

            <xsl:element name="aud:Loja">
                <!-- com os dados disponibilizados pelos profs, so temos o id e nome da loja... o resto que está
                nos schemas que foi adicionado (a pedido dos mesmos) é completamente useless... [NOTA: eu ja corrigi
                 isto, basicamente algumas coisas ficaram com o minOccurs="0"... -->
                <xsl:attribute name="IDLoja">
                    <xsl:value-of select="/root/DadosVenda/Store/text()"/>
                </xsl:attribute>
                <xsl:element name="loj:Nome">
                    <xsl:value-of select="/root/DadosVenda/StoreName/text()"/>
                </xsl:element>
            </xsl:element>

            <xsl:element name="aud:Venda">
                <xsl:attribute name="dataInicio">
                    <xsl:value-of select="/root/DadosVenda/OrderDate/text()"/>
                </xsl:attribute>

                <xsl:element name="aud:Moeda">
                    <xsl:attribute name="idTaxaCambio">
                        <xsl:choose>
                            <xsl:when test="contains(/root/DadosVenda/CurrencyRateID/text(),$checker)">
                                <xsl:text>0</xsl:text>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="/root/DadosVenda/CurrencyRateID/text()"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>

                    <xsl:if test="not(contains(/root/DadosVenda/CurrencyRateID/text(),$checker))">
                        <xsl:attribute name="dataTaxaCambio">
                            <xsl:value-of select="/root/DadosMoeda/CurrencyRateDate/text()"/>
                        </xsl:attribute>
                    </xsl:if>

                    <xsl:choose>
                        <xsl:when test="contains(/root/DadosVenda/CurrencyRateID/text(),$checker)">
                            <xsl:text>USD</xsl:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="/root/DadosMoeda/ToCurrencyCode/text()"/>
                        </xsl:otherwise>
                    </xsl:choose>

                </xsl:element>

                <xsl:element name="aud:DadosVenda">
                    <xsl:attribute name="IDVenda">
                        <xsl:value-of select="/root/DadosVenda/ReceiptID/text()"/>
                    </xsl:attribute>

                    <xsl:element name="vnd:InformacaoAdicionalVenda">
                        <!-- Adicionar as informações relativas ao schema das informações -->
                    </xsl:element>

                    <xsl:element name="vnd:SubTotal">
                        <xsl:value-of select="/root/DadosVenda/SubTotal/text()"/>
                    </xsl:element>
                    <xsl:element name="vnd:TaxaImposto">
                        <xsl:value-of select="/root/DadosVenda/TaxAmt/text()"/>
                    </xsl:element>
                    <xsl:element name="vnd:Cliente">
                        <xsl:attribute name="IDCliente">
                            <xsl:value-of select="/root/DadosVenda/Customer/text()"/>
                        </xsl:attribute>
                    </xsl:element>

                    <xsl:element name="vnd:LinhasVenda">
                        <xsl:for-each select="/root/DadosVenda/ReceiptLines">
                            <!--
                            Variável que possibilita aceder a filhos deste for-each dentro de outro for-each aninhado
                            -->
                            <xsl:variable name="nestedCicleParent" select="."/>
                            <xsl:element name="vnd:DadosLinha">
                                <xsl:attribute name="IDLinha">
                                    <xsl:value-of select="ReceiptLineID/text()"/>
                                </xsl:attribute>

                                <xsl:element name="vnd:Produto">
                                    <!--
                                    Verifica a igualdade dos ProductIDs relativos à informação dos 2 CSV (basicamente
                                    escreve a informação do CSV dos produtos relativa ao ProductID do node() atual
                                    do primeiro for-each...
                                    -->
                                    <xsl:for-each select="//DadosProdutos/DadosProduto">
                                        <xsl:if test="contains($nestedCicleParent/ProductID/text(),ProductID/text())">
                                            <xsl:attribute name="IDProduto">
                                                <xsl:value-of select="ProductID/text()"/>
                                            </xsl:attribute>

                                            <xsl:element name="prd:Codigo">
                                                <xsl:value-of select="ProductNumber/text()"/>
                                            </xsl:element>
                                            <xsl:element name="prd:PrecoEmLista">
                                                <xsl:value-of select="ListPrice/text()"/>
                                            </xsl:element>
                                            <xsl:element name="prd:Nome">
                                                <xsl:value-of select="SellStartDate/text()"/>
                                            </xsl:element>
                                            <xsl:element name="prd:Cor">
                                                <xsl:value-of select="Color/text()"/>
                                            </xsl:element>

                                            <xsl:if test="not(contains(SellStartDate/text(),$checker))">
                                                <xsl:element name="prd:dataInicioVenda">
                                                    <xsl:value-of select="SellStartDate/text()"/>
                                                </xsl:element>
                                            </xsl:if>
                                            <xsl:if test="not(contains(SellEndDate/text(),$checker))">
                                                <xsl:element name="prd:dataFinalVenda">
                                                    <xsl:value-of select="SellEndDate/text()"/>
                                                </xsl:element>
                                            </xsl:if>
                                        </xsl:if>
                                    </xsl:for-each>
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

            <xsl:element name="aud:InformacaoAdicional"/>
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>
