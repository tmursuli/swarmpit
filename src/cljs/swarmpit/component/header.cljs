(ns swarmpit.component.header
  (:require [material.components :as comp]
            [material.icon :as icon]
            [swarmpit.routes :as routes]
            [swarmpit.storage :as storage]
            [swarmpit.event.source :as eventsource]
            [swarmpit.component.common :as common]
            [swarmpit.component.state :as state]
            [swarmpit.component.mixin :as mixin]
            [swarmpit.url :refer [dispatch!]]
            [rum.core :as rum]
            [sablono.core :refer-macros [html]]
            [clojure.string :as string]
            [goog.crypt :as crypt])
  (:import [goog.crypt Md5]))

(enable-console-print!)

(defn- user-gravatar-hash [email]
  (let [md5 (Md5.)]
    (when (some? email)
      (do
        (.update md5 (string/trim email))
        (crypt/byteArrayToHex (.digest md5))))))

(defn- user-avatar
  ([]
   (user-avatar false))
  ([big?]
   (comp/avatar
     (merge
       {:className "Swarmpit-appbar-avatar"
        :src       (str "https://eu.ui-avatars.com/api/?name=" (storage/user))}
       (when big?
         {:className "Swarmpit-appbar-avatar Swarmpit-appbar-avatar-big"})))))

(rum/defc user-menu < rum/static [anchorEl]
  (comp/box
    {}
    (comp/icon-button
      {:onClick       (fn [e]
                        (state/update-value [:menuAnchorEl] (.-currentTarget e) state/layout-cursor))
       :disableRipple true
       :color         "inherit"} (user-avatar))
    (comp/popper
      {:open       (some? anchorEl)
       :anchorEl   anchorEl
       :placement  "bottom-end"
       :className  "Swarmpit-popper"
       :transition true}
      (fn [props]
        (let [{:keys [TransitionProps placement]} (js->clj props :keywordize-keys true)]
          (comp/fade
            (merge TransitionProps
                   {:timeout 450})
            (comp/paper
              {}
              (comp/click-away-listener
                {:onClickAway #(state/update-value [:menuAnchorEl] nil state/layout-cursor)}
                (comp/card
                  {}
                  (comp/card-content
                    {:className "Swarmpit-appbar-user-menu"}
                    (user-avatar true)
                    (comp/typography
                      {:variant "body1"} (storage/user))
                    (comp/typography
                      {:variant "body2"} (storage/email))
                    (comp/button
                      {:variant   "outlined"
                       :className "Swarmpit-appbar-user-menu-action"
                       :onClick   (fn []
                                    (state/update-value [:menuAnchorEl] nil state/layout-cursor)
                                    (dispatch!
                                      (routes/path-for-frontend :account-settings)))}
                      "Manage your account"))
                  (comp/divider {})
                  (comp/card-actions
                    {:className "Swarmpit-appbar-user-menu"}
                    (comp/button
                      {:variant   "outlined"
                       :startIcon (icon/exit {})
                       :onClick   (fn []
                                    (storage/remove "token")
                                    (eventsource/close!)
                                    (state/update-value [:menuAnchorEl] nil state/layout-cursor)
                                    (dispatch!
                                      (routes/path-for-frontend :login)))}
                      "Sign out")))))))))))

(defn- mobile-actions-menu
  [actions mobileMoreAnchorEl]
  (comp/menu
    {:id              "Swarmpit-appbar-action-menu"
     :key             "Swarmpit-appbar-action-menu"
     :anchorEl        mobileMoreAnchorEl
     :anchorOrigin    {:vertical   "top"
                       :horizontal "right"}
     :transformOrigin {:vertical   "top"
                       :horizontal "right"}
     :open            (some? mobileMoreAnchorEl)
     :onClose         #(state/update-value [:mobileMoreAnchorEl] nil state/layout-cursor)}
    (->> actions
         (map #(comp/menu-item
                 {:key      (str "mobile-menu-item-" (:name %))
                  :disabled (:disabled %)
                  :onClick  (fn []
                              ((:onClick %))
                              (state/update-value [:mobileMoreAnchorEl] nil state/layout-cursor))}
                 (comp/list-item-icon
                   {:key (str "mobile-menu-item-icon-" (:name %))} (:icon %))
                 (comp/typography
                   {:variant "inherit"
                    :key     (str "mobile-menu-item-text-" (:name %))} (:name %)))))))

(rum/defc search-input < rum/reactive [on-change-fn title]
  (let [{:keys [query]} (state/react state/search-cursor)]
    (html
      [:div.Swarmpit-appbar-search
       [:div.Swarmpit-appbar-search-icon (icon/search {})]
       (comp/input
         {:placeholder      (str "Search " (string/lower-case title) " ...")
          :onChange         on-change-fn
          :defaultValue     query
          :type             "search"
          :fullWidth        true
          :classes          {:root  "Swarmpit-appbar-search-root"
                             :input "Swarmpit-appbar-search-input"}
          :id               "Swarmpit-appbar-search-filter"
          :key              "appbar-search"
          :disableUnderline true})])))

(rum/defc mobile-search-message < rum/reactive [on-change-fn title]
  (let [{:keys [query]} (state/react state/search-cursor)]
    (html
      [:span#snackbar-mobile-search.Swarmpit-appbar-search-mobile-message
       (comp/input
         {:placeholder      (str "Search " (string/lower-case title) " ...")
          :onChange         on-change-fn
          :defaultValue     query
          :type             "search"
          :fullWidth        true
          :classes          {:root  "Swarmpit-appbar-search-mobile-root"
                             :input "Swarmpit-appbar-search-mobile-input"}
          :disableUnderline true})])))

(defn- mobile-search-action []
  (html
    [:span
     (comp/icon-button
       {:onClick    #(state/update-value [:mobileSearchOpened] false state/layout-cursor)
        :key        "search-btn-close"
        :aria-label "Close"
        :color      "inherit"}
       (icon/close
         {:className "Swarmpit-appbar-search-mobile-close"}))]))

(defn- mobile-search [on-change-fn title opened]
  (comp/snackbar
    {:open         opened
     :anchorOrigin {:vertical   "top"
                    :horizontal "center"}
     :className    "Swarmpit-appbar-search-mobile"
     :onClose      #(state/update-value [:mobileSearchOpened] false state/layout-cursor)}
    (comp/snackbar-content
      {:aria-describedby "snackbar-mobile-search"
       :className        "Swarmpit-appbar-search-mobile-content"
       :classes          {:message "Swarmpit-appbar-search-mobile-content-message"}
       :message          (mobile-search-message on-change-fn title)
       :action           (mobile-search-action)})))

(rum/defc appbar-mobile-section < rum/static [search-fn actions]
  (html
    [:div.Swarmpit-appbar-section-mobile
     (when search-fn
       (comp/icon-button
         {:key           "menu-search"
          :aria-haspopup "true"
          :onClick       #(state/update-value [:mobileSearchOpened] true state/layout-cursor)
          :color         "inherit"} (icon/search {})))
     (when (some? actions)
       (comp/icon-button
         {:key           "menu-more"
          :aria-haspopup "true"
          :onClick       (fn [e]
                           (state/update-value [:mobileMoreAnchorEl] (.-currentTarget e) state/layout-cursor))
          :color         "inherit"} icon/more))]))

(rum/defc appbar-desktop-section < rum/static [search-fn actions title]
  (html
    [:div.Swarmpit-appbar-section-desktop
     (when search-fn
       (search-input search-fn title))
     (->> actions
          (filter #(or (nil? (:disabled %))
                       (false? (:disabled %))))
          (map #(comp/tooltip
                  {:title (:name %)
                   :key   (str "menu-tooltip-" (:name %))}
                  (comp/icon-button
                    {:color   "inherit"
                     :key     (str "menu-btn-" (:name %))
                     :onClick (:onClick %)} (:icon %)))))]))

(defonce appbar-elevation (atom 0))

(def mixin-on-scroll
  {:did-mount
   (fn [state]
     (.addEventListener
       js/window
       "scroll"
       (fn [_]
         (let [top? (zero? (-> js/window .-scrollY))]
           (if top?
             (reset! appbar-elevation 0)
             (reset! appbar-elevation 4))))) state)})

(rum/defc appbar < rum/reactive
                   mixin-on-scroll [{:keys [title subtitle search-fn actions]}]
  (let [{:keys [mobileSearchOpened menuAnchorEl mobileMoreAnchorEl version]} (state/react state/layout-cursor)
        elevation (rum/react appbar-elevation)]
    (comp/mui
      (html
        [:div
         (comp/app-bar
           {:key       "appbar"
            :color     "primary"
            :elevation elevation
            :id        "Swarmpit-appbar"
            :className "Swarmpit-appbar"}
           (comp/toolbar
             {:key            "appbar-toolbar"
              :disableGutters false}
             (html
               [:div.Swarmpit-desktop-title
                (common/title-logo)
                (common/title-version version)])
             (comp/icon-button
               {:key        "appbar-menu-btn"
                :color      "inherit"
                :aria-label "Open drawer"
                :onClick    #(state/update-value [:mobileOpened] true state/layout-cursor)
                :className  "Swarmpit-appbar-menu-btn"}
               icon/menu)
             (comp/typography
               {:key       "appbar-title"
                :className "Swarmpit-appbar-title"
                :variant   "h6"
                :color     "inherit"
                :noWrap    true}
               title)
             (html [:div.grow])
             (appbar-desktop-section search-fn actions title)
             (appbar-mobile-section search-fn actions)
             (user-menu menuAnchorEl)))
         (mobile-actions-menu actions mobileMoreAnchorEl)
         (mobile-search search-fn title mobileSearchOpened)]))))
