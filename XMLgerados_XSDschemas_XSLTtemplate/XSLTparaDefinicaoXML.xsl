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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" version="1.0"
                xmlns:doc="ProjetoPEI/Grupo4/EntregaFinal/Documento"
                xmlns:inf="ProjetoPEI/Grupo4/Entrega2/InformacaoAdicional"
                xmlns:aud="ProjetoPEI/Grupo4/EntregaFinal/Auditoria"
                xmlns:loj="ProjetoPEI/Grupo4/EntregaFinal/Loja"
                xmlns:vnd="ProjetoPEI/Grupo4/EntregaFinal/Venda"
                xmlns:prd="ProjetoPEI/Grupo4/EntregaFinal/Produto" extension-element-prefixes="doc inf">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>
    <!-- Variável que vai ser utilizada em condições relativas aos elementos que têm possibilidade ser ter como valor
     'NULL' (como as datas e a cor do produto) -->
    <xsl:variable name="checker" select="'NULL'"/>

    <xsl:template match="/">
        <xsl:element name="doc:Documento">
            <!-- Adição do atributo para a definição do 'schema instance' e os namespaces dos schemas para validação. -->
            <xsl:attribute name="xsi:schemaLocation">
                <!-- O XSLT guarda os enters (&#10;), portanto não se pode usar o 'enter' na string abaixo ... -->
                <xsl:text>ProjetoPEI/Grupo4/Entrega2/InformacaoAdicional ../SchemasInformacaoAdicional/InformacaoAdicional.xsd ProjetoPEI/Grupo4/EntregaFinal/Documento ../SchemasDefinicaoModulos/DocumentoValidacao.xsd ProjetoPEI/Grupo4/EntregaFinal/Auditoria ../SchemasDefinicaoModulos/SchemaAuditoriaLoja.xsd ProjetoPEI/Grupo4/EntregaFinal/Loja ../SchemasDefinicaoModulos/SchemaLoja.xsd ProjetoPEI/Grupo4/EntregaFinal/Venda ../SchemasDefinicaoModulos/SchemaVenda.xsd ProjetoPEI/Grupo4/EntregaFinal/Produto ../SchemasDefinicaoModulos/SchemaProduto.xsd</xsl:text>
            </xsl:attribute>

            <xsl:element name="doc:Auditoria">
                <!-- Ainda nao sei como vamos adicionar o atributo da data de criação do documento... -->
                <xsl:element name="aud:Loja">
                    <!-- com os dados disponibilizados pelos profs, so temos o id e nome da loja... o resto que está
                    nos schemas que foi adicionado é completamente useless... [NOTA: eu ja corrigi isto, basicamente
                    algumas coisas ficaram com o minOccurs="0"... -->
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

                    <xsl:element name="vnd:Moeda">
                        <!-- Quando a taxa de câmbio é NULL, o id é colocado a 0 e a moeda será o USD -->
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

                    <xsl:element name="vnd:DadosVenda">
                        <xsl:attribute name="IDVenda">
                            <xsl:value-of select="/root/DadosVenda/ReceiptID/text()"/>
                        </xsl:attribute>

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
                                        do primeiro for-each (referente à venda)...
                                        -->
                                        <xsl:for-each select="//DadosProdutos/DadosProduto">
                                            <!-- Procura pela ocorrencia do ProductID da venda na informação sobre os
                                             produtos dentro do elemento <DadosProdutos> no XML -->
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
                                                <!-- Adiciona a data só quando não se encontra a 'NULL' -->
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

                    <xsl:element name="vnd:InformacaoAdicional">
                        <!-- Informações relativas à venda -->
                        <!--
                        <xsl:element name="vnd:NumeroProdutos">
                            <xsl:element name="vnd:Total">
                                <xsl:value-of select="/root/Informacao/Venda/Total/text()"/>
                            </xsl:element>
                            <xsl:element name="vnd:TotalDiferentes">
                                <xsl:value-of select="/root/Informacao/Venda/TotalDiferentes/text()"/>
                            </xsl:element>
                        </xsl:element>

                        <xsl:element name="vnd:PrecoMedioVendaProdutos">
                            <xsl:value-of select="/root/Informacao/Venda/PrecoMedioVendaProdutos/text()"/>
                        </xsl:element>
                        -->
                    </xsl:element>
                </xsl:element>

                <xsl:element name="aud:InformacaoAdicional">
                    <!-- Informações relativas ao exercício -->
                    <!--
                    <xsl:element name="aud:NumeroProdutos">
                        <xsl:element name="aud:Total">
                            <xsl:value-of select="/root/Informacao/Exercicio/Total/text()"/>
                        </xsl:element>
                        <xsl:element name="aud:TotalDiferentes">
                            <xsl:value-of select="/root/Informacao/Exercicio/TotalDiferentes/text()"/>
                        </xsl:element>
                    </xsl:element>

                    <xsl:element name="aud:NumeroTotalClientesDiferentes">
                        <xsl:value-of select="/root/Informacao/Exercicio/TotalClientesDiferentes/text()"/>
                    </xsl:element>

                    <xsl:element name="aud:ValorVendidoPorCliente">
                        <xsl:for-each select="/root/Informacao/Exercicio/ValorVendidoCliente">
                            <xsl:element name="aud:Cliente">
                                <xsl:attribute name="id">
                                    <xsl:value-of select="IDCliente/text()"/>
                                </xsl:attribute>
                                <xsl:value-of select="Valor/text()"/>
                            </xsl:element>
                        </xsl:for-each>
                    </xsl:element>

                    <xsl:element name="aud:TotalProdutosVendidos">
                        <xsl:for-each select="/root/Informacao/Exercicio/TotalVendidoProduto">
                            <xsl:element name="aud:Produto">
                                <xsl:attribute name="id">
                                    <xsl:value-of select="IDProduto/text()"/>
                                </xsl:attribute>
                                <xsl:value-of select="Valor/text()"/>
                            </xsl:element>
                        </xsl:for-each>
                    </xsl:element>

                    <!- Aqui é necessário ir ao CSV do currencyDetails para saber qual é a moeda, tendo em conta o
                     CurrencyRateID. Aqui está resolvido da mesma forma que se resolveu no CurrencyRateID relativo à
                     auditoria da loja que este XML vai representar. No entanto, o que foi feito na API, para esta situação,
                     foi colocar 3 elementos (CurrencyRateID, Identificação da moeda e o valor vendido pela moeda)
                     no elemento <TotalVendidoMoeda>, assim é possível fazer a verificação do "NULL" aqui ->
                    <xsl:element name="aud:ValorTotalVendidoPorMoeda">
                        <xsl:for-each select="/root/Informacao/Exercicio/TotalVendidoMoeda">
                            <xsl:element name="aud:Moeda">
                                <xsl:attribute name="codigo">
                                    <xsl:choose>
                                        <xsl:when test="contains(CurrencyRateID/text(),$checker)">
                                            <xsl:text>USD</xsl:text>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="Moeda/text()"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:attribute>
                                <xsl:value-of select="Valor/text()"/>
                            </xsl:element>
                        </xsl:for-each>
                    </xsl:element>
                    -->
                </xsl:element>
            </xsl:element>

            <xsl:element name="inf:InformacaoAdicional">
                <!-- Informações adicionais relativas às lojas, no exercicio -->
                <xsl:element name="doc:TotalProdutosVendidos">
                    <xsl:for-each select="/root/Informacao/TotalProdutosVendidosPorLoja">
                        <xsl:element name="doc:Loja">
                            <xsl:attribute name="id">
                                <xsl:value-of select="_id/text()"/>
                            </xsl:attribute>
                            <xsl:value-of select="valor/text()"/>
                        </xsl:element>
                    </xsl:for-each>
                </xsl:element>

                <xsl:element name="inf:ValorTotalVendas">
                    <xsl:for-each select="/root/Informacao/ValorTotalVendas">
                        <xsl:element name="doc:Loja">
                            <xsl:attribute name="id">
                                <xsl:value-of select="_id/text()"/>
                            </xsl:attribute>
                            <xsl:value-of select="valor/text()"/>
                        </xsl:element>
                    </xsl:for-each>
                </xsl:element>

                <xsl:element name="inf:ValorMedioPrecoVendaProdutos">
                    <xsl:for-each select="/root/Informacao/ValorMedioPrecoVendaProdutos">
                        <xsl:element name="doc:Loja">
                            <xsl:attribute name="id">
                                <xsl:value-of select="_id/text()"/>
                            </xsl:attribute>
                            <xsl:value-of select="valor/text()"/>
                        </xsl:element>
                    </xsl:for-each>
                </xsl:element>
            </xsl:element>
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>
