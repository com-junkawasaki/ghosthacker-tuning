(ns ghosthacker-tuning.core
  "GHOST HACKER: TUNING -- alignment-puzzle core (ADR-2607023200 addendum
  2: portfolio title #6, Nei's second title after ECHOES).

  Pure, host-free judgment/state engine: Nei tunes a log's misaligned
  frequency dial to its target, channel by channel. Unlike FLOW/HARMONY
  (ghosthacker-groove-core), there is no beat grid or real-time pressure
  here -- TUNING is about precision, not reflexes, matching Nei's
  observational role (:gh/observationEye) rather than Ren's action
  titles. `nudge-dial` moves a [0.0, 1.0] dial by a signed delta;
  `lock-in` judges the dial against the current channel's target with
  the same three-tier accuracy model the rest of the portfolio uses
  (:perfect / :good / :miss), then advances to the next channel. No
  rendering or input I/O lives here -- those are host adapters layered
  on top, same split as every other title in this portfolio."
  )

(defn clamp01 [x] (max 0.0 (min 1.0 x)))

(def initial-dial
  "既定の初期ダイヤル位置(中央、0.5)。的から見て『まだ合っていない』方に
   寄せず、フラットな中立から始める。"
  0.5)

(defn nudge-dial
  "dialをdelta分動かし[0.0, 1.0]にclampする。"
  [dial delta]
  (clamp01 (+ dial delta)))

(def perfect-window
  "既定の:perfect判定窓(ダイヤル値の絶対差)。"
  0.015)
(def good-window
  "既定の:good判定窓(ダイヤル値の絶対差)。"
  0.05)

(defn- magnitude [x] (if (neg? x) (- x) x))

(defn judge-with-windows
  "ズレ(dial - target、符号は問わない)を、渡されたperfect/good判定窓で判定する。"
  [delta perfect good]
  (let [abs-delta (magnitude (double delta))]
    (cond
      (<= abs-delta perfect) :perfect
      (<= abs-delta good) :good
      :else :miss)))

(defn judge-alignment
  "ズレ(dial - target)から判定を返す(既定窓)。"
  [delta]
  (judge-with-windows delta perfect-window good-window))

(def initial-state
  {:score 0
   :combo 0
   :max-combo 0
   :judgments []
   :channel-index 0})

(def ^:private base-score {:perfect 1000 :good 400 :miss 0})

(defn apply-judgment
  "judgmentをstateに反映する。:missはcomboをリセット、:perfect/:goodは
   comboを伸ばす。channel-indexを1つ進める。"
  [state judgment]
  (let [next-combo (if (= judgment :miss) 0 (inc (:combo state)))]
    (-> state
        (update :judgments conj judgment)
        (assoc :combo next-combo)
        (update :max-combo max next-combo)
        (update :score + (get base-score judgment))
        (update :channel-index inc))))

(defn current-channel
  "channels(ベクタ、各要素{:label :target})のうち、stateが現在調整対象と
   すべきチャンネルを返す。全チャンネル完了後はnil。"
  [state channels]
  (nth channels (:channel-index state) nil))

(defn complete?
  "state(channelsに対する)が全チャンネルを消化し終えたか。"
  [state channels]
  (>= (:channel-index state) (count channels)))

(defn lock-in
  "現在チャンネルのtargetに対してdialを判定し、stateに適用する。
   全チャンネル消化済みならstateをそのまま返す(呼び出し側の責任で
   complete?を先にチェックすること)。"
  [state channels dial]
  (if (complete? state channels)
    state
    (let [target (:target (current-channel state channels))]
      (apply-judgment state (judge-alignment (- dial target))))))

(defn accuracy
  "judgmentsのうち:perfect/:goodが占める割合(0.0〜1.0)。judgmentsが空なら
   1.0(未プレイをミス扱いにしない)。"
  [state]
  (let [judgments (:judgments state)]
    (if (empty? judgments)
      1.0
      (/ (count (remove #(= % :miss) judgments)) (double (count judgments))))))

(defn grade
  "accuracyから最終評価を返す。"
  [state]
  (let [acc (accuracy state)]
    (cond
      (>= acc 0.95) :perfect-tune
      (>= acc 0.8) :a
      (>= acc 0.6) :b
      (>= acc 0.4) :c
      :else :d)))

(defn summary
  "runの結果サマリ。ホストアダプタ側のリザルト画面にそのまま渡せる形。"
  [state]
  {:score (:score state)
   :max-combo (:max-combo state)
   :accuracy (accuracy state)
   :grade (grade state)
   :judgment-count (count (:judgments state))})

(defn play
  "channelsに対し、dialsを順にlock-inで適用する。dialsが尽きても全
   チャンネル終わっていなければ、そこまでのstateで打ち切る(ホスト
   アダプタが途中で中断した場合に相当)。"
  [channels dials]
  (loop [state initial-state
         ds (seq dials)]
    (if (or (complete? state channels) (nil? ds))
      state
      (recur (lock-in state channels (first ds)) (next ds)))))

(defn play-summary
  "play + summaryの合成。ホストアダプタが1run分のdial列を録り終えた後に
   呼ぶ最短経路。"
  [channels dials]
  (summary (play channels dials)))
