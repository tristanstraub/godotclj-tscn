(ns godotclj.tscn
  (:require [instaparse.core :as instaparse]
            [camel-snake-kebab.core :as csk]
            [clojure.string :as str]))

(defn parser
  [s & args]
  (let [p (apply instaparse/parser
                 "
    file       = <ws*> attributes? <ws*> descriptor (<ws*> descriptor <ws*>)*;
    descriptor = header (<ws*> kv)*;
    header     = <'['> <ws*> identifier (<ws+> attributes)? <ws*> <']'>;
    attributes = kv (<ws+> kv)*;
    kv         = identifier <ws*> <'='> <ws*> value;
    identifier = #'[0-9a-zA-Z_/]+';
    value      = integer | number | object | string | dictionary | boolean | array;
    boolean    = 'true' | 'false';
    object     = identifier <'('> <ws*> arguments? <ws*> <')'>;
    integer    = #'[+-]'? #'[0-9]+';
    number     = #'[+-]'? #'[0-9]+' '.' #'[0-9]+';
    string     = <'\"'> #'[^\"]*' <'\"'>;
    arguments  = value (<ws*> <','> <ws*> value)* (<ws*> <','>)?;
    dictionary = <'{'> keyvalues <ws*> <'}'>;
    keyvalues  = (<ws*> dictionarykv <ws*> (<','> <ws*> dictionarykv)*)?;
    array      = <'['> <ws*> arguments? <ws*> <']'>;
    dictionarykv = string <ws*> <':'> <ws*> value;
    ws         = #'[ \t\n]+' | ';' #'[^\n]*[\n]?';
"
                 args)]
    (p s)))

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
   :header       (fn
                   ([resource]
                    {:resource   resource})
                   ([resource attributes]
                    {:resource   resource
                     :attributes attributes}))
   :identifier   (fn [vs] vs)
   :value        (fn [vs] vs)
   :attributes   (fn [& kv] (into {} kv))
   :dictionary   (fn
                   ([] nil)
                   ([kv] (into {} kv)))
   :keyvalues    (fn [& kv] kv)
   :array        (fn
                   ([] [])
                   ([values] values))
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
  [s & args]
  (instaparse/transform transforms (apply parser s args)))

(comment
  (instaparse/transform transforms (parser "[gd_scene load_steps=3 format=2]"))

  (parse "[gd_scene load_steps=3 format=2]" :start :header)

  (parse (slurp "../thecreeps-godot-clj/src/godot/HUD.tscn"))

  (parse (slurp "../thecreeps-godot-clj/project.godot"))

  (parse "{\n}" :start :dictionary)

  (parse "config_version=4" :start :attributes)

  )
