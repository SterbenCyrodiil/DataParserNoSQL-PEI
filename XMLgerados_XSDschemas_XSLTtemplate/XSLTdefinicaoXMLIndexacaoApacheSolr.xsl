<?xml version="1.0" encoding="utf-8"?>
<!---->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>
    <!-- Variável que vai ser utilizada em condições relativas aos elementos que têm possibilidade ser ter como valor
     'NULL' (como as datas e a cor do produto) -->
    <xsl:variable name="checker" select="'NULL'"/>

    <xsl:template match="/">
        <add>
            <xsl:for-each select="/root/DadosVenda/ReceiptLines">
                <xsl:variable name="receiptLine" select="."/>
                <doc>
                    <!-- Fields relativos aos dados da Loja -->
                    <xsl:element name="field">
                        <xsl:attribute name="name">
                            <xsl:text>idLoja</xsl:text>
                        </xsl:attribute>
                        <xsl:value-of select="//DadosVenda/Store/text()"/>
                    </xsl:element>
                    <xsl:element name="field">
                        <xsl:attribute name="name">
                            <xsl:text>nomeLoja</xsl:text>
                        </xsl:attribute>
                        <xsl:value-of select="//DadosVenda/StoreName/text()"/>
                    </xsl:element>

                    <!-- Fields relativos aos dados da Venda -->
                    <xsl:element name="field">
                        <xsl:attribute name="name">
                            <xsl:text>dataVenda</xsl:text>
                        </xsl:attribute>
                        <xsl:value-of select="//DadosVenda/OrderDate/text()"/>
                    </xsl:element>
                    <xsl:element name="field">
                        <xsl:attribute name="name">
                            <xsl:text>idVenda</xsl:text>
                        </xsl:attribute>
                        <xsl:value-of select="//DadosVenda/ReceiptID/text()"/>
                    </xsl:element>

                    <!-- Fields relativos aos dados da Moeda -->
                    <xsl:element name="field">
                        <xsl:attribute name="name">
                            <xsl:text>moeda</xsl:text>
                        </xsl:attribute>
                        <xsl:choose>
                            <xsl:when test="contains(//DadosVenda/CurrencyRateID/text(),$checker)">
                                <xsl:text>USD</xsl:text>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="//DadosMoeda/ToCurrencyCode/text()"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:element>
                    <xsl:if test="not(contains(//DadosVenda/CurrencyRateID/text(),$checker))">
                        <xsl:element name="field">
                            <xsl:attribute name="name">
                                <xsl:text>idTaxaCambio</xsl:text>
                            </xsl:attribute>
                            <xsl:value-of select="//DadosVenda/CurrencyRateID/text()"/>
                        </xsl:element>

                        <xsl:element name="field">
                            <xsl:attribute name="name">
                                <xsl:text>dataTaxaCambio</xsl:text>
                            </xsl:attribute>
                            <xsl:value-of select="//DadosMoeda/CurrencyRateDate/text()"/>
                        </xsl:element>
                    </xsl:if>

                    <!-- Fields relativos aos dados da Venda -->
                    <xsl:element name="field">
                        <xsl:attribute name="name">
                            <xsl:text>subTotal</xsl:text>
                        </xsl:attribute>
                        <xsl:value-of select="//DadosVenda/SubTotal/text()"/>
                    </xsl:element>
                    <xsl:element name="field">
                        <xsl:attribute name="name">
                            <xsl:text>taxaImposto</xsl:text>
                        </xsl:attribute>
                        <xsl:value-of select="//DadosVenda/TaxAmt/text()"/>
                    </xsl:element>
                    <xsl:element name="field">
                        <xsl:attribute name="name">
                            <xsl:text>cliente</xsl:text>
                        </xsl:attribute>
                        <xsl:value-of select="//DadosVenda/Customer/text()"/>
                    </xsl:element>

                    <!-- Fields relativos aos dados de uma Linha de Venda -->
                    <xsl:element name="field">
                        <xsl:attribute name="name">
                            <xsl:text>idLinhaVenda</xsl:text>
                        </xsl:attribute>
                        <xsl:value-of select="ReceiptLineID/text()"/>
                    </xsl:element>

                    <!-- Fields relativos aos dados do Produto referente à Linha de Venda -->
                    <xsl:for-each select="//DadosProdutos/DadosProduto">
                        <!-- Procura pela ocorrencia do ProductID da venda na informação sobre os
                         produtos dentro do elemento <DadosProdutos> no XML -->
                        <xsl:if test="contains($receiptLine/ProductID/text(),ProductID/text())">
                            <xsl:element name="field">
                                <xsl:attribute name="name">
                                    <xsl:text>idProduto</xsl:text>
                                </xsl:attribute>
                                <xsl:value-of select="ProductID/text()"/>
                            </xsl:element>
                            <xsl:element name="field">
                                <xsl:attribute name="name">
                                    <xsl:text>codigoProduto</xsl:text>
                                </xsl:attribute>
                                <xsl:value-of select="ProductNumber/text()"/>
                            </xsl:element>
                            <xsl:element name="field">
                                <xsl:attribute name="name">
                                    <xsl:text>precoEmListaProduto</xsl:text>
                                </xsl:attribute>
                                <xsl:attribute name="multiValued">
                                    <xsl:text>false</xsl:text>
                                </xsl:attribute>
                                <xsl:attribute name="type">
                                    <xsl:text>float</xsl:text>
                                </xsl:attribute>
                                <xsl:value-of select="ListPrice/text()"/>
                            </xsl:element>
                            <xsl:element name="field">
                                <xsl:attribute name="name">
                                    <xsl:text>nomeProduto</xsl:text>
                                </xsl:attribute>
                                <xsl:value-of select="Name/text()"/>
                            </xsl:element>
                            <!-- Adiciona a cor ou a data só quando não se encontra a 'NULL' -->
                            <xsl:if test="not(contains(Color/text(),$checker))">
                                <xsl:element name="field">
                                    <xsl:attribute name="name">
                                        <xsl:text>corProduto</xsl:text>
                                    </xsl:attribute>
                                    <xsl:value-of select="Color/text()"/>
                                </xsl:element>
                            </xsl:if>
                            <xsl:if test="not(contains(SellStartDate/text(),$checker))">
                                <xsl:element name="field">
                                    <xsl:attribute name="name">
                                        <xsl:text>dataInicioVendaProduto</xsl:text>
                                    </xsl:attribute>
                                    <xsl:value-of select="SellStartDate/text()"/>
                                </xsl:element>
                            </xsl:if>
                            <xsl:if test="not(contains(SellEndDate/text(),$checker))">
                                <xsl:element name="field">
                                    <xsl:attribute name="name">
                                        <xsl:text>dataFimVendaProduto</xsl:text>
                                    </xsl:attribute>
                                    <xsl:value-of select="SellEndDate/text()"/>
                                </xsl:element>
                            </xsl:if>
                        </xsl:if>
                    </xsl:for-each>

                    <xsl:element name="field">
                        <xsl:attribute name="name">
                            <xsl:text>precoPorUnidade</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="multiValued">
                            <xsl:text>false</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="type">
                            <xsl:text>float</xsl:text>
                        </xsl:attribute>
                        <xsl:value-of select="UnitPrice/text()"/>
                    </xsl:element>
                    <xsl:element name="field">
                        <xsl:attribute name="name">
                            <xsl:text>quantidadeProduto</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="multiValued">
                            <xsl:text>false</xsl:text>
                        </xsl:attribute>

                        <xsl:value-of select="Quantity/text()"/>
                    </xsl:element>
                    <xsl:element name="field">
                        <xsl:attribute name="name">
                            <xsl:text>totalLinha</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="multiValued">
                            <xsl:text>false</xsl:text>
                        </xsl:attribute>
                        <xsl:attribute name="type">
                            <xsl:text>float</xsl:text>
                        </xsl:attribute>
                        <xsl:value-of select="LineTotal/text()"/>
                    </xsl:element>
                </doc>
            </xsl:for-each>
        </add>
    </xsl:template>
</xsl:stylesheet>
