from flask_restful import Api
from app import flaskAppInstance

from .Recommend import Recommend
from .ml_utils import *

restServer = Api(flaskAppInstance)

restServer.add_resource(Recommend, "/api/v1.0/recommend", resource_class_kwargs={ 'root_path': "saved_objects" })