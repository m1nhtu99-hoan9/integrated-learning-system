(ns integrated-learning-system.views.layouts
  "Shared layout components"
  (:use [hiccup.core]))


;region navbar

(def ^:private div-navbar-more-items
  [:div.navbar-item.has-dropdown.is-hoverable
   [:a.navbar-link "More"]
   [:div.navbar-dropdown
    [:a.navbar-item {:href "/swagger"}
     "Developer API References"]
    [:hr.navbar-divider]
    [:a.navbar-item "About Us"]]])

(def ^:private navbar-burger
  ; hidden for laptop screen, shown in smaller screens
  [:a.navbar-burger {:role        "button"
                     :aria-label  "menu" :aria-expanded "false"
                     :data-target "navbar-menu-contents"}
   [:span {:aria-hidden "true"}]
   [:span {:aria-hidden "true"}]
   [:span {:aria-hidden "true"}]])

(defn navbar [{:as props}]
  [:nav.navbar.is-fixed-top {:role "navigation" :aria-label "main navigation"}
   [:div.navbar-brand
    [:a.navbar-item {:href "/"}
     [:span#brand-title "Integrated Learning System"]]
    navbar-burger]
   [:div.navbar-menu {:id "navbar-menu-contents"}
    [:div.navbar-start
     [:a.navbar-item {:href "/timetable"} "Timetable"]
     [:a.navbar-item "Classes"]
     div-navbar-more-items]
    [:div.navbar-end
     [:div.navbar-item
      [:div.buttons
       [:a.button.is-primary
        [:strong "Sign up"]]
       [:a.button.is-light "Log in"]]]]]])

;endregion

;region banners

(defn errors-banner [{:keys [title errors]
                      :or {errors []}}]
  (html
    [:section.hero.is-danger.is-medium
     [:div.hero-body
      [:p.title
       [:span.icon-text
        [:span.icon
         [:i.fas.fa-exclamation-circle]]
        [:span (h title)]]]
      [:div.content
       [:ul
        (for [[field-name error-messages] errors]
          [:li
           [:code (h field-name)]
           [:ul (for [error-message error-messages]
                  [:li error-message])]])]]]]))

(defn param-errors-banner [{:as param-errors}]
  (errors-banner {:title "URL contains invalid parameters"
                  :errors param-errors}))

;endregion
