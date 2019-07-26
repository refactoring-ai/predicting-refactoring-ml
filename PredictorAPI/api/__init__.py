from flask_restful import Api
from app import flaskAppInstance

from .Recommend import Recommend

restServer = Api(flaskAppInstance)

restServer.add_resource(Recommend, "/api/v1.0/recommend")