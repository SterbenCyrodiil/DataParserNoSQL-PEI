<?xml version="1.0" encoding="utf-8"?>
<!-- O primeiro Stylesheet com os dados encontrados através da pesquisa realizada no mongoDB
(ainda vai ser adicionada mais informação nos produtos)

EU ESTOU COMPLETAMENTE LIXADO PARA ISTO, nao percebo nada de como vamos fazer isto. Por exemplo, a pesquisa da loja
naquele mês devolve vários JSON com as várias linhas de venda, mas com várias, outras, cenas repetidas...
Não sei como se vai traduzir isso para o XSLT que vai fazer os XML...
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:template match="/">

        <xsl:element name="Auditoria" namespace="ProjetoPEI/Grupo4/Entrega2/Auditoria">
            <!-- adicionar o atributo da data de criação... Como? tambem queria saber -->

            <xsl:element name="Loja" namespace="ProjetoPEI/Grupo4/Entrega2/Auditoria">
                <!-- com os dados disponibilizados pelos profs, so temos o id e nome da loja... o resto que está
                nos schemas que foi adicionado (a pedido dos mesmos) é completamente useless... -->
                <xsl:attribute name="IDLoja" namespace="ProjetoPEI/Grupo4/Entrega1/Loja">
                    <xsl:value-of select="/root/Store"/>
                </xsl:attribute>
                <xsl:element name="Nome" namespace="ProjetoPEI/Grupo4/Entrega1/Loja">
                    <xsl:value-of select="/root/StoreName"/>
                </xsl:element>
            </xsl:element>


        </xsl:element>
    </xsl:template>
</xsl:stylesheet>
