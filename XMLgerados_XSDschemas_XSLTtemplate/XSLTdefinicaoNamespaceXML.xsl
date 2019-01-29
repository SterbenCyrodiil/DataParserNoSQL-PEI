<?xml version="1.0" encoding="utf-8"?>
<!--
Este Stylesheet vai no fundo definir uma template definitiva, com namespaces, para os XML a serem gerados, para que
seja dessa forma possível fazer a validação com o vocabulário definido.
Isto é feito com base na xml String que vai ser criada na API (com o uso da API 'org.json.XML' e de várias pesquisas
realizadas no mongoDB pelas várias collections).

(NOTA: A abordagem seguida para aceder aos dados foi juntar toda a informação dos 3 CSV na mesma XML String
podendo asseder-se ao conteúdo específico através do uso do elemento relativo (para usar os dados dos produtos -
"/DadosProdutos/<elemento>/text()" e das moedas "/DadosMoeda..." e a informação contida na venda "DadosVenda" -
isto só é possivel utilizando a <xsl:template> no elemento "/root", como se pode ver abaixo...)

(NOTA: resolvido, a nova alteração na query junta todas as receipt lines num documento embutido!)
*comentário_resolvido*: EU ESTOU COMPLETAMENTE CONFUSO, nao percebo nada de como vamos fazer isto. Por exemplo, a pesquisa
da loja naquele mês devolve vários JSON com as várias linhas de venda, mas com várias, outras, coisas repetidas...
Não sei como se vai traduzir isso para um só XSLT, porque se for para dividir isso tudo em vários XSLTs vai ser uma
complexidade desgraçada na API depois...
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:doc="ProjetoPEI/Grupo4/EntregaFinal/Documento"
                xmlns:inf="ProjetoPEI/Grupo4/Entrega2/InformacaoAdicional"
                xmlns:iexc="ProjetoPEI/Grupo4/EntregaFinal/InformacaoAdicionalAuditorias"
                xmlns:ivnd="ProjetoPEI/Grupo4/EntregaFinal/InformacaoAdicionalVenda"
                xmlns:aud="ProjetoPEI/Grupo4/EntregaFinal/Auditoria" xmlns:loj="ProjetoPEI/Grupo4/EntregaFinal/Loja"
                xmlns:vnd="ProjetoPEI/Grupo4/EntregaFinal/Venda" xmlns:prd="ProjetoPEI/Grupo4/EntregaFinal/Produto"
                exclude-result-prefixes="doc" version="1.0">
    <xsl:output method="xml" indent="yes" encoding="utf-8"/>
    <!-- Controlo de whitespaces nos elementos -->
    <xsl:strip-space elements="*"/>
    <xsl:preserve-space elements="loj:Nome prd:Nome"/>

    <!-- Variável que vai ser utilizada em condições relativas aos elementos que têm possibilidade ter como valor
     'NULL' (como as datas e a cor do produto) -->
    <xsl:variable name="checker" select="'NULL'"/>
    <!-- Nova estrutura completa do documento XML -->
    <xsl:template match="/">
        <xsl:element name="doc:Documento">

            <!-- Adição do atributo para a definição do 'schema instance' e os namespaces dos schemas para validação. -->
            <xsl:attribute name="xsi:schemaLocation">
                <!-- O XSLT guarda os enters (&#10;), portanto não se pode usar o 'enter' na string abaixo ... -->
                <xsl:text>ProjetoPEI/Grupo4/Entrega2/InformacaoAdicional ../SchemasInformacaoAdicional/InformacaoAdicional.xsd ProjetoPEI/Grupo4/EntregaFinal/InformacaoAdicionalVenda ../SchemasInformacaoAdicional/InformacaoAdicionalVenda.xsd ProjetoPEI/Grupo4/EntregaFinal/InformacaoAdicionalAuditorias ../SchemasInformacaoAdicional/InformacaoAdicionalAuditorias.xsd ProjetoPEI/Grupo4/EntregaFinal/Documento ../SchemasDefinicaoModulos/DocumentoValidacao.xsd ProjetoPEI/Grupo4/EntregaFinal/Auditoria ../SchemasDefinicaoModulos/SchemaAuditoriaLoja.xsd ProjetoPEI/Grupo4/EntregaFinal/Loja ../SchemasDefinicaoModulos/SchemaLoja.xsd ProjetoPEI/Grupo4/EntregaFinal/Venda ../SchemasDefinicaoModulos/SchemaVenda.xsd ProjetoPEI/Grupo4/EntregaFinal/Produto ../SchemasDefinicaoModulos/SchemaProduto.xsd</xsl:text>
            </xsl:attribute>

            <xsl:element name="doc:Auditoria">
                <!-- Ainda nao sei como vamos adicionar o atributo da data de criação do documento... -->
                <xsl:element name="aud:Loja">
                    <!-- com os dados disponibilizados pelos profs, so temos o id e nome da loja...
                    [NOTA: ja está corrigido, basicamente algumas coisas ficaram com o minOccurs="0"... -->
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
                                <!-- Variável que possibilita aceder a filhos deste for-each dentro de outro for-each
                                aninhado (neste caso o for-each para encontrar o produto relacionado) -->
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
                                             produtos dentro do elemento <DadosProdutos> no XML (esta abordagem é feia,
                                             mas não tenho mais ideias nesta altura) -->
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
                                                    <xsl:value-of select="Name/text()"/>
                                                </xsl:element>
                                                <!-- Adiciona a cor ou a data só quando não se encontram a 'NULL' -->
                                                <xsl:if test="not(contains(Color/text(),$checker))">
                                                    <xsl:element name="prd:Cor">
                                                        <xsl:value-of select="Color/text()"/>
                                                    </xsl:element>
                                                </xsl:if>
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

                    <xsl:element name="vnd:InformacaoAdicionalVenda">
                        <!-- Informações relativas à venda -->
                        <xsl:element name="ivnd:NumeroProdutos">
                            <xsl:element name="ivnd:Total">
                                <xsl:value-of select="/root/Informacao/TotalProdutosVenda/valor/text()"/>
                            </xsl:element>
                            <xsl:element name="ivnd:TotalDiferentes">
                                <xsl:value-of select="/root/Informacao/TotalProdutosDiferentesVenda/valor/text()"/>
                            </xsl:element>
                        </xsl:element>

                        <xsl:element name="ivnd:PrecoMedioVendaProdutos">
                            <xsl:value-of select="/root/Informacao/MediaPrecoVendaProdutosVenda/valor/text()"/>
                        </xsl:element>
                    </xsl:element>
                </xsl:element>

                <xsl:element name="aud:InformacaoAdicionalExercicio">
                    <!-- Informações relativas ao exercício (auditorias) -->
                    <xsl:element name="iexc:NumeroProdutos">
                        <xsl:element name="iexc:Total">
                            <xsl:value-of select="/root/Informacao/TotalProdutosExercicio/valor/text()"/>
                        </xsl:element>
                        <xsl:element name="iexc:TotalDiferentes">
                            <xsl:value-of select="/root/Informacao/TotalProdutosDiferentesExercicio/valor/text()"/>
                        </xsl:element>
                    </xsl:element>

                    <xsl:element name="iexc:NumeroTotalClientesDiferentes">
                        <xsl:value-of select="/root/Informacao/TotalClientesDiferentesExercicio/valor/text()"/>
                    </xsl:element>

                    <xsl:element name="iexc:ValorVendidoPorCliente">
                        <xsl:for-each select="/root/Informacao/ValorVendidoClienteExercicio">
                            <xsl:element name="iexc:Cliente">
                                <xsl:attribute name="id">
                                    <xsl:value-of select="_id/text()"/>
                                </xsl:attribute>
                                <xsl:value-of select="valor/text()"/>
                            </xsl:element>
                        </xsl:for-each>
                    </xsl:element>

                    <xsl:element name="iexc:TotalProdutosVendidos">
                        <xsl:for-each select="/root/Informacao/TotalUnidadesVendidasProdutoExercicio">
                            <xsl:element name="iexc:Produto">
                                <xsl:attribute name="id">
                                    <xsl:value-of select="_id/text()"/>
                                </xsl:attribute>
                                <xsl:value-of select="valor/text()"/>
                            </xsl:element>
                        </xsl:for-each>
                    </xsl:element>

                    <!-- Aqui é necessário ir ao CSV do currencyDetails para saber qual é a moeda, tendo em conta o
                     CurrencyRateID. Está resolvido da mesma forma que se resolveu no CurrencyRateID relativo à
                     auditoria da loja que este XML vai representar. No entanto, o que foi feito na API, para esta situação,
                     foi colocar 3 elementos (CurrencyRateID, Identificação da moeda e o valor vendido pela moeda)
                     no elemento <TotalVendaPorMoedaExercicio>, assim é possível fazer a verificação do "NULL" aqui -->
                    <xsl:element name="iexc:ValorTotalVendidoPorMoeda">
                        <xsl:for-each select="/root/Informacao/TotalVendaPorMoedaExercicio">
                            <xsl:element name="iexc:Moeda">
                                <xsl:attribute name="codigo">
                                    <xsl:choose>
                                        <xsl:when test="contains(CurrencyIDValorTotal/_id/text(),$checker)">
                                            <xsl:text>USD</xsl:text>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="DadosMoeda/ToCurrencyCode/text()"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:attribute>
                                <xsl:value-of select="CurrencyIDValorTotal/valor/text()"/>
                            </xsl:element>
                        </xsl:for-each>
                    </xsl:element>
                </xsl:element>
            </xsl:element>

            <xsl:element name="doc:InformacaoAdicional">
                <!-- Informações adicionais relativas às lojas, no exercicio -->
                <xsl:element name="inf:TotalProdutosVendidos">
                    <xsl:for-each select="/root/Informacao/TotalProdutosVendidosPorLoja">
                        <xsl:element name="inf:Loja">
                            <xsl:attribute name="id">
                                <xsl:value-of select="_id/text()"/>
                            </xsl:attribute>
                            <xsl:value-of select="valor/text()"/>
                        </xsl:element>
                    </xsl:for-each>
                </xsl:element>

                <xsl:element name="inf:ValorTotalVendas">
                    <xsl:for-each select="/root/Informacao/ValorTotalVendasPorLoja">
                        <xsl:element name="inf:Loja">
                            <xsl:attribute name="id">
                                <xsl:value-of select="_id/text()"/>
                            </xsl:attribute>
                            <xsl:value-of select="valor/text()"/>
                        </xsl:element>
                    </xsl:for-each>
                </xsl:element>

                <xsl:element name="inf:ValorMedioPrecoVendaProdutos">
                    <xsl:for-each select="/root/Informacao/MediaPrecoVendaProdutosPorLoja">
                        <xsl:element name="inf:Loja">
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
