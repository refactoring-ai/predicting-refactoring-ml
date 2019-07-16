# Predicting refactoring

Notes (to be improved in this README):

* Arquivo que guardamos no before e after tem o mesmo nome do arquivo final. Em alguns casos, como Rename Class, significa que o código antigo da classe ficará salvo num arquivo com o "novo nome" da classe. Isso não influencia em nada o treinamento.

* O CSV que geramos contém duplicações. Nós limpamos essas duplicações na mão e criamos PKs e FKs. O dump do banco que disponibilizaremos é o melhor a ser usado.


## Data collection

When running in scale, some projects might fail. Although the app tries to remove problematic rows, if the process dies, partially loaded projects will be in the database. Use the following queries to clean up half-baked projects:

```
delete from yes where project_id in (select id from project where finishedDate is null);
delete from no where project_id in (select id from project where finishedDate is null);
delete from project where finishedDate is null;
```

## Traditional ML

```
pip3 install --user -r requirements.txt
python3 warm_cache.py
python3 main.py
```
