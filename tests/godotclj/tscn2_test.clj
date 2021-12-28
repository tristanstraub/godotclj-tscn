(ns godotclj.tscn2-test
  (:require [clojure.test :refer [deftest is testing]]
            [godotclj.tscn2 :refer [parse emit default-options]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn input-is-output
  ([input options]
   (let [content (slurp input)]
     (= content (emit options (parse content)))))
  ([input]
   (input-is-output input default-options)))

(comment
  (defn emit-temp
    ([input options]
     (spit (str "tmp/" input) (emit options (parse (slurp (io/resource (str "thecreeps/" input)))))))
    ([input]
     (emit-temp input default-options)))

  (do
    (emit-temp "project.godot" (assoc-in default-options [:attribute :spacing] ""))
    (emit-temp "default_env.tres")
    (emit-temp "HUD.gdns")
    (emit-temp "HUD.tscn")
    (emit-temp "libgodotclj.gdnlib" (assoc-in default-options [:attribute :spacing] ""))
    (emit-temp "main.gdns")
    (emit-temp "main.tscn")
    (emit-temp "Mob.gdns")
    (emit-temp "Mob.tscn")
    (emit-temp "Player.gdns")
    (emit-temp "Player.tscn"))

  (parse (slurp (io/resource "thecreeps/Mob.tscn"))))

(deftest emit-blocks-test
  (is (input-is-output (io/resource "thecreeps/project.godot")
                       (assoc-in default-options [:attribute :spacing] "")))
  (is (input-is-output (io/resource "thecreeps/HUD.gdns")))
  (is (input-is-output (io/resource "thecreeps/HUD.tscn")))
  (is (input-is-output (io/resource "thecreeps/libgodotclj.gdnlib")
                       (assoc-in default-options [:attribute :spacing] "")))
  (is (input-is-output (io/resource "thecreeps/main.gdns")))
  (is (input-is-output (io/resource "thecreeps/main.tscn")))
  (is (input-is-output (io/resource "thecreeps/Mob.gdns")))
  (is (input-is-output (io/resource "thecreeps/Mob.tscn")))
  (is (input-is-output (io/resource "thecreeps/Player.gdns")))
  (is (input-is-output (io/resource "thecreeps/Player.tscn")))





  )
