(ns magewars-spellbook.systems
  (:require 
   [magewars-spellbook.handler :refer [app]]
   [environ.core :refer [env]]
   [system.core :refer [defsystem]]
   (system.components 
    [http-kit :refer [new-web-server]])))

(defsystem dev-system
  [:web (new-web-server (Integer. (env :http-port)) app)])

(defsystem prod-system
  [:web (new-web-server (Integer. (env :http-port)) app)])
