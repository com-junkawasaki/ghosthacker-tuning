# GHOST HACKER: TUNING

![test](https://github.com/com-junkawasaki/ghosthacker-tuning/actions/workflows/test.yml/badge.svg)

Ghost Hacker ゲームポートフォリオ第6弾。設計は
[ADR-2607023200](../../../90-docs/adr/2607023200-ghosthacker-game-portfolio-flow.md)
（superproject `com-junkawasaki/root`、addendum 2）を参照。

[Ghost Hacker](https://github.com/com-junkawasaki/ghosthacker)（既存カノン: Ren/Nei、
「情報は物理だ」、情報場、Ghost Battle / Daemon Battle）を土台に、FreeTEMPOの
『The World Is Echoed』（2003）収録曲 "Tuning" に由来する、10ジャンル展開の
第6弾。

## コンセプト

- **ジャンル**: パズル（整合パズル）
- **主人公**: Nei単独
- **コアループ**: 噛み合わない複数のログ（周波数）を、ダイヤルを微調整して
  目標値に合わせる。FLOW/HARMONY（`ghosthacker-groove-core`）とは違い
  リアルタイムのビート判定は無い — Neiの観察眼（`:gh/observationEye`）が
  活きる、反射神経ではなく精度を競うパズル。ダイヤルと目標のズレ(絶対値)が
  `:perfect` / `:good` / `:miss` に判定され、全チャンネルを消化した時点の
  正答率でgradeが決まる。

## 実装範囲

`src/ghosthacker_tuning/core.cljc` — pure、host-free。判定/state核:

- `nudge-dial` — ダイヤルを`[0.0, 1.0]`にclampしながら動かす
- `judge-alignment` — ダイヤルと目標値のズレ(絶対値)を`:perfect`/`:good`/
  `:miss`に判定（`ghosthacker-groove-core`とは独立実装 — TUNINGの判定
  モデルはビートグリッド前提ではないため、リズム系titleの共有libには
  乗せていない）
- `lock-in`/`current-channel`/`complete?` — 現在チャンネルを判定して次へ
  進める、全チャンネル消化判定
- `accuracy`/`grade`/`summary` — リザルト画面向けのサマリ

`src/ghosthacker_tuning/logs.cljc` — サンプルの完結したパズル
（`quiet-static`、4チャンネル）。

**プレイ可能な最小プロトタイプ**として `src/ghosthacker_tuning/terminal.clj`
がある。FLOW/HARMONYと違い実時間のビート判定が無いため、`future`/agent
スレッドプールを一切使わない素朴なnudge&lockの REPLループ。目標値は
直接表示せず、`static`（近さのみを示す0〜100%のノイズ量、方向は教えない）
だけを手がかりに、耳で合わせるような手触りにしている。

**ブラウザで遊べるホストアダプタ**が `src/ghosthacker_tuning/web.cljs`
（reagent、ADR-2607100900 follow-up (b)）: TUNINGはリアルタイムの
ビート判定が無いため、ECHOESと同じ低複雑度側の構成（Web Audio不要、
ボタン駆動のnudge/lock UI）で足りる。

## 開発

```bash
clojure -M:test
```

Lint（clj-kondo、Clojars経由でHomebrew等の別インストール不要）:

```bash
clojure -M:lint
```

`main`へのpush/PRで `.github/workflows/test.yml` が自動でテスト+lintを実行する。

ターミナルで遊んでみる:

```bash
clojure -M -m ghosthacker-tuning.terminal
```

ブラウザで遊んでみる（`npm install`は初回のみ）:

```bash
npm install
npx shadow-cljs watch app   # http://localhost:8296 で自動リロード開発
npx shadow-cljs release app # public/ に静的バンドルをビルド(デプロイ可能)
```

変更履歴は [CHANGELOG.md](CHANGELOG.md)。

## ライセンス

MIT License — [LICENSE](LICENSE) 参照。
