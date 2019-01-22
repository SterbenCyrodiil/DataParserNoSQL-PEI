<?xml version="1.0" encoding="utf-8"?>
<!-- O primeiro Stylesheet com os dados encontrados através da pesquisa realizada no mongoDB
(ainda vai ser adicionada mais informação nos produtos, moeda e as informações adicionais ...)

EU ESTOU COMPLETAMENTE LIXADO PARA ISTO, nao percebo nada de como vamos fazer isto. Por exemplo, a pesquisa da loja
naquele mês devolve vários JSON com as várias linhas de venda, mas com várias, outras, cenas repetidas...
Não sei como se vai traduzir isso para o XSLT que vai fazer os XML... (NOTA: resolvido, ver a alteração da query!)
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:aud='ProjetoPEI/Grupo4/Entrega2/Auditoria'
                xmlns:loj='ProjetoPEI/Grupo4/EntregaFinal/Loja'
                xmlns:prd='ProjetoPEI/Grupo4/EntregaFinal/Produto'
                xmlns:vnd='ProjetoPEI/Grupo4/Entrega2/Venda'>
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>

    <xsl:template match="/">
        <!-- ainda nao sei como vamos adicionar o atributo da data de criação do documento... -->
        <xsl:element name="aud:Auditoria">

            <xsl:element name="aud:Loja">
                <!-- com os dados disponibilizados pelos profs, so temos o id e nome da loja... o resto que está
                nos schemas que foi adicionado (a pedido dos mesmos) é completamente useless... [NOTA: eu ja corrigi
                 isto, basicamente algumas coisas sao SIM useless e ficaram com o minOccurs="0". -->
                <xsl:attribute name="IDLoja">
                    <xsl:value-of select="/root/Store/text()"/>
                </xsl:attribute>
                <xsl:element name="loj:Nome">
                    <xsl:value-of select="/root/StoreName/text()"/>
                </xsl:element>
            </xsl:element>

            <xsl:element name="aud:Venda">
                <xsl:element name="aud:Moeda">
                    <xsl:attribute name="idTaxaCambio">
                        <xsl:value-of select="/root/CurrencyRateID/text()"/>
                    </xsl:attribute>
                    <!-- Adicionar o value-of da moeda correspondente ao currencyRateID e a data (atributo) -->
                </xsl:element>

                <xsl:element name="aud:DadosVenda">
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

                <xsl:attribute name="dataInicio">
                    <xsl:value-of select="/root/OrderDate/text()"/>
                </xsl:attribute>
            </xsl:element>

        </xsl:element>
    </xsl:template>
</xsl:stylesheet>
