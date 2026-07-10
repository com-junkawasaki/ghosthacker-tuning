(ns ghosthacker-tuning.terminal-test
  "-mainそのものはテストせず、private var経由でstatic-pct/tune-channel!/
   play-loop!を直接叩く。実プロセスとしての-main自体は手動検証済み
   （完走/q途中打ち切り/不正コマンドの再入力要求、いずれも正しく完了し
   プロセスがハングしないことを確認）。"
  (:require [clojure.test :refer [deftest is testing]]
            [ghosthacker-tuning.core :as core]
            [ghosthacker-tuning.logs :as logs]
            [ghosthacker-tuning.terminal :as terminal]))

(def ^:private static-pct #'terminal/static-pct)
(def ^:private tune-channel! #'terminal/tune-channel!)
(def ^:private play-loop! #'terminal/play-loop!)

(defn- silently [thunk]
  (binding [*out* (java.io.StringWriter.)]
    (thunk)))

(defn- close-to? [a b] (< (Math/abs (- (double a) (double b))) 1e-9))

(deftest static-pct-boundary-test
  (testing "ぴったりなら0%、0.5以上ズレていれば頭打ちで100%"
    (is (= 0 (static-pct 0.5 0.5)))
    (is (= 100 (static-pct 1.0 0.5)))
    (is (= 100 (static-pct 0.0 0.9)))
    (is (= 50 (static-pct 0.75 0.5)))))

(deftest tune-channel-lock-and-quit-test
  (let [channel {:label :test :target 0.5}]
    (testing "+/-でnudgeし、lでその時点のdialをロックインする"
      (let [dial (silently #(with-in-str "+\n+\nl\n" (tune-channel! channel)))]
        (is (close-to? 0.6 dial))))
    (testing "空行でもロックインできる"
      (let [dial (silently #(with-in-str "+\n\n" (tune-channel! channel)))]
        (is (close-to? 0.55 dial))))
    (testing "qまたはEOFで打ち切り(nil)"
      (is (nil? (silently #(with-in-str "q\n" (tune-channel! channel)))))
      (is (nil? (silently #(with-in-str "+\n" (tune-channel! channel))))))
    (testing "不正コマンドは読み飛ばし、次の有効なコマンドを処理する"
      (let [dial (silently #(with-in-str "xyz\nl\n" (tune-channel! channel)))]
        (is (close-to? 0.5 dial))))))

(deftest play-loop-eof-boundary-test
  (testing "全チャンネルを完璧にロックインし切れば完走する"
    (let [state (silently
                 #(with-in-str "l\nl\nl\nl\n"
                    (play-loop! [{:label :a :target core/initial-dial}
                                 {:label :b :target core/initial-dial}])))]
      (is (core/complete? state [{:label :a :target core/initial-dial}
                                  {:label :b :target core/initial-dial}]))
      (is (= [:perfect :perfect] (:judgments state)))))
  (testing "途中でq/EOFになれば、そこまでのstateで打ち切る(未完走)"
    (let [channels logs/quiet-static
          state (silently #(with-in-str "l\nq\n" (play-loop! channels)))]
      (is (not (core/complete? state channels)))
      (is (= 1 (:channel-index state))))))
