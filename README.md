# Predicting refactoring

Notes (to be improved in this README):

* Arquivo que guardamos no before e after tem o mesmo nome do arquivo final. Em alguns casos, como Rename Class, significa que o código antigo da classe ficará salvo num arquivo com o "novo nome" da classe. Isso não influencia em nada o treinamento.

* O CSV que geramos contém duplicações. Nós limpamos essas duplicações na mão e criamos PKs e FKs. O dump do banco que disponibilizaremos é o melhor a ser usado.

## Traditional ML

pip3 install -r requirements.txt
python3 warm_cache.py
python3 main.py
