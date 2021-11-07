(ns godotclj.tscn
  (:require [instaparse.core :as instaparse]
            [camel-snake-kebab.core :as csk]
            [clojure.string :as str]))

(def parser
  (instaparse/parser
   "file       = descriptor (<ws*> descriptor <ws*>)*;
    descriptor = header (<ws*> kv)*;
    header     = <'['> <ws*> identifier (<ws+> attributes)? <ws*> <']'>;
    attributes = kv (<ws+> kv)*;
    kv         = identifier <ws*> <'='> <ws*> value;
    identifier = #'[0-9a-zA-Z_/]+';
    value      = integer | number | object | string | dictionary | boolean | array;
    boolean    = 'true' | 'false';
    object     = identifier <'('> <ws*> arguments? <ws*> <')'>
    integer    = #'[+-]'? #'[0-9]+';
    number     = #'[+-]'? #'[0-9]+' '.' #'[0-9]+';
    string     = <'\"'> #'[^\"]*' <'\"'>;
    arguments  = value (<ws*> <','> <ws*> value)* (<ws*> <','>)?
    dictionary = <'{'> (<ws*> dictionarykv <ws*> (<','> <ws*> dictionarykv)*) <ws*> <'}'>
    array      = <'['> <ws*> arguments? <ws*> <']'>
    dictionarykv = string <ws*> <':'> <ws*> value;
    ws         = #'[ \t\n]+';
"))

(defn ->keyword-maybe
  [k]
  (if (str/starts-with? k "_")
    k
    (csk/->kebab-case-keyword k :separator \_)))

(def transforms
  {:file         (fn [& parts] (vec parts))
   :kv           (fn [k v]
                   [(->keyword-maybe k) v])
   :resource     (fn [name values] {:name name :values values})
   :descriptor   (fn [resource & values]
                   (if (seq values)
                     (assoc resource :values (into {} values))
                     resource))
   :header       (fn [resource attributes]
                   {:resource   resource
                    :attributes attributes})
   :identifier   (fn [vs] vs)
   :value        (fn [vs] vs)
   :attributes   (fn [& kv] (into {} kv))
   :dictionary   (fn [& kv] (into {} kv))
   :array        (fn [values] values)
   :dictionarykv (fn [k v]
                   [(->keyword-maybe k) v])

   :number       (fn [& value]
                   (Double/parseDouble (apply str value)))
   :integer      (fn [& value]
                   (Integer/parseInt (apply str value)))
   :string       (fn [v] v)
   :boolean      (fn [v] (= v "true"))
   :object       (fn [name args]
                   {:name name :arguments args})
   :arguments    (fn [& args]
                   (vec args))})

(defn parse
  [s]
  (instaparse/transform transforms (parser s)))

(comment
  (instaparse/transform transforms (parser "[gd_scene load_steps=3 format=2]"))

  (instaparse/transform transforms (parser (slurp "../thecreeps-godot-clj/src/godot/HUD.tscn")))

  )
