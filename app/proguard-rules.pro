# Mantém os nomes de classes e métodos: o erro que o app mostra ao fechar (botão Copiar erro)
# continua legível, sem precisar de arquivo de mapeamento. O R8 ainda remove o código não usado.
-dontobfuscate
-keepattributes SourceFile,LineNumberTable
