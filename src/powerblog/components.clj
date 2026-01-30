(ns powerblog.components)

(def navbar
  [:nav {:class "bg-primary-400"}
   [:div {:class "flex flex-wrap items-center justify-between mx-auto py-4 px-8"}
    ;; Logo section
    [:a {:href "/" :class "flex items-center space-x-3"}
     [:img {:src "/favicon.svg" :class "h-8" :alt "LinZiHao's Digital Garden"}]
     [:span {:class "self-center text-2xl font-semibold whitespace-nowrap"}
      "LinZiHao's Digital Garden"]]

    ;; Mobile menu button
    [:button {:type "button"
              :class "inline-flex items-center p-2 w-10 h-10 justify-center text-sm rounded-lg md:hidden hover:bg-primary-100"
              :aria-controls "navbar-default"
              :aria-expanded "false"
              :id "mobile-menu-button"}
     [:span {:class "sr-only"} "打开菜单"]
     [:svg {:class "w-5 h-5" :aria-hidden "true" :xmlns "http://www.w3.org/2000/svg"
            :fill "none" :viewBox "0 0 17 14"}
      [:path {:stroke "currentColor" :stroke-linecap "round" :stroke-linejoin "round"
              :stroke-width "2" :d "M1 1h15M1 7h15M1 13h15"}]]]

    ;; Navigation links
    [:div {:class "w-full md:block md:w-auto hidden" :id "navbar-default"}
     [:ul {:class "flex flex-col p-4 md:p-0 mt-4 border md:flex-row md:space-x-8 md:mt-0 md:border-0 items-center"}
      [:li
       [:a {:href "/" :class "block py-2 px-3 text-gray-900 rounded hover:bg-gray-100 md:hover:bg-transparent md:border-0 md:hover:text-blue-700 md:p-0"}
        "Home"]]
      [:li
       [:a {:href "/about" :class "block py-2 px-3 text-gray-900 rounded hover:bg-gray-100 md:hover:bg-transparent md:border-0 md:hover:text-blue-700 md:p-0"}
        "About"]]]]]])

(def navbar-script
  "document.getElementById('mobile-menu-button').addEventListener('click', function() {
    var menu = document.getElementById('navbar-default');
    menu.classList.toggle('hidden');
  });")

(def footer
  [:footer {:class "w-full bg-gray-100 py-8"}
   [:div {:class "container mx-auto px-4"}
    [:div {:class "flex flex-col items-center justify-center space-y-4"}
     ;; Social links
     [:nav {:class "flex flex-wrap justify-center gap-4"}
      [:a {:href "https://www.linkedin.com/in/zihao-lin-8b8067326/"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-gray-600 hover:text-gray-900 transition-colors"}
       [:span {:class "sr-only"} "LinkedIn"]
       "LinkedIn"]
      [:a {:href "https://x.com/fTJSaF2VnI11762"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-gray-600 hover:text-gray-900 transition-colors"}
       [:span {:class "sr-only"} "X (Twitter)"]
       "X"]
      [:a {:href "https://space.bilibili.com/375039815"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-gray-600 hover:text-gray-900 transition-colors"}
       [:span {:class "sr-only"} "Bilibili"]
       "Bilibili"]
      [:a {:href "https://www.zhihu.com/people/lin-zi-hao-8-89"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-gray-600 hover:text-gray-900 transition-colors"}
       [:span {:class "sr-only"} "Zhihu"]
       "Zhihu"]
      [:a {:href "https://github.com/schneiderlin"
           :target "_blank" :rel "noopener noreferrer"
           :class "text-gray-600 hover:text-gray-900 transition-colors"}
       [:span {:class "sr-only"} "GitHub"]
       "GitHub"]]
     ;; Copyright
     [:p {:class "text-sm text-gray-500"}
      (str "© " (.getYear (java.time.LocalDate/now)) " LinZiHao's Digital Garden. All rights reserved.")]]]])
