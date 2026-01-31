(ns powerblog.render
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [powerpack.hiccup :as hiccup]
   [powerpack.markdown :as md]
   [powerblog.components :as components]
   [powerblog.lint :as lint]))

;;; ---------------------------------------------------------------------------
;;; Data / query (used only by render)
;;; ---------------------------------------------------------------------------

(defn get-blog-posts [db]
  (->> (d/q '[:find [?e ...]
              :where
              [?e :blog-post/author]]
            db)
       (map #(d/entity db %))))

(defn get-posts-by-tag [db tag]
  (->> (d/q '[:find [?e ...]
              :in $ ?tag
              :where
              [?e :blog-post/tags ?tag]]
            db tag)
       (map #(d/entity db %))))

(defn get-all-tags [db]
  (->> (d/q '[:find [?tag ...]
              :where
              [_ :blog-post/tags ?tag]]
            db)
       set))

;;; ---------------------------------------------------------------------------
;;; Body / TOC helpers
;;; ---------------------------------------------------------------------------

(defn body-to-html-string
  "Render markdown body to a plain HTML string (for regex/TOC and safe embedding via unescape)."
  [body]
  (when (string? body)
    (->> body
         str/split-lines
         md/unindent-but-first
         (str/join "\n")
         (md/md-to-html))))

(defn extract-headings [html]
  (let [pattern #"<h([2-6])[^>]*id=\"([^\"]+)\"[^>]*>(.*?)</h\1>"
        matches (re-seq pattern html)]
    (mapv (fn [[_ depth slug text]]
            {:depth (Integer/parseInt depth)
             :slug slug
             :text (str/replace text #"<[^>]+>" "")})
          matches)))

(defn build-toc [headings]
  (loop [headings headings
         toc []
         parents {}]
    (if-let [h (first headings)]
      (let [depth (:depth h)]
        (cond
          (= depth 2)
          (recur (rest headings)
                 (conj toc (assoc h :subheadings []))
                 (assoc parents depth h))

          :else
          (if-let [parent (get parents (dec depth))]
            (let [new-parent (update parent :subheadings conj h)]
              (recur (rest headings)
                     toc
                     (assoc parents depth new-parent)))
            (recur (rest headings) toc parents))))
      toc)))

(defn render-toc-item [heading]
  (let [is-h2 (= (:depth heading) 2)
        base-class (if is-h2
                     "block py-2 px-3 rounded-md text-gray-600 hover:text-primary-600 hover:bg-primary-50 transition-colors duration-200"
                     "block py-1 pl-6 text-sm text-gray-600 hover:text-primary-600 transition-colors duration-200")]
    (list
     [:a {:href (str "#" (:slug heading))
          :class base-class
          :data-heading (:slug heading)}
      (:text heading)]

     (when (seq (:subheadings heading))
       [:div
        (for [sub (:subheadings heading)]
          (render-toc-item sub))]))))

;;; ---------------------------------------------------------------------------
;;; Layout (split head/body to reduce nesting)
;;; ---------------------------------------------------------------------------

(defn layout-head [{:keys [title description]}]
  [:head
   [:meta {:charset "UTF-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
   [:meta {:name "description" :content (or description "Blog")}]
   (when title [:title title])
   [:link {:rel "icon" :type "image/svg+xml" :href "/favicon.svg"}]
   ;; Google Fonts (in HTML to avoid Optimus inline-CSS blocking external URLs)
   [:link {:rel "stylesheet" :href "https://fonts.googleapis.com/css2?family=Crimson+Text:ital,wght@0,400;0,600;0,700;1,400&family=JetBrains+Mono:wght@400;500&family=Noto+Serif+SC:wght@400;500;600;700&display=swap"}]
   [:link {:rel "stylesheet" :href "/styles.css"}]
   [:style "
      #progress-bar {
        --progress: 0;
        position: fixed;
        top: 0;
        left: 0;
        height: 3px;
        width: var(--progress);
        background-color: var(--primary-600);
        z-index: 9999;
        transition: width 0.1s ease-out;
      }
      .line-clamp-3 {
        display: -webkit-box;
        -webkit-line-clamp: 3;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }
    "]])

(def progress-bar-script
  "(function() {
    const progressBar = document.getElementById('progress-bar');
    if (progressBar) {
      function updateProgressBar() {
        const scrollTop = document.documentElement.scrollTop || document.body.scrollTop;
        const scrollHeight = document.documentElement.scrollHeight - document.documentElement.clientHeight;
        const scrollPercent = scrollHeight > 0 ? (scrollTop / scrollHeight) * 100 : 0;
        progressBar.style.setProperty('--progress', scrollPercent + '%');
      }
      document.addEventListener('scroll', updateProgressBar, { passive: true });
      window.addEventListener('resize', updateProgressBar);
      updateProgressBar();
    }
  })();")

(defn layout-body [content]
  [:body {:class "min-h-screen flex flex-col"}
   components/navbar
   [:main {:class "flex-1"} content]
   components/footer
   [:script components/navbar-script]
   [:script progress-bar-script]])

(defn layout [{:keys [lang] :as opts} & content]
  [:html {:lang (or lang "en")}
   (layout-head opts)
   (layout-body content)])

;;; ---------------------------------------------------------------------------
;;; Cards / UI building blocks
;;; ---------------------------------------------------------------------------

(defn blog-post-card [post]
  [:a {:href (:page/uri post)
       :class "card card-hoverable block post"
       :data-tags (str/join "," (:blog-post/tags post))}
   [:div {:class "p-6 space-y-4"}
    [:h3 {:class "card-title text-lg font-semibold text-gray-900 leading-tight"}
     (:page/title post)]
    [:div {:class "flex flex-wrap gap-2"}
     (for [tag (take 3 (:blog-post/tags post))]
       [:span {:class "badge badge-neutral"}
        (name tag)])]
    [:p {:class "text-sm text-gray-600 leading-relaxed line-clamp-3"}
     (:page/description post "")]
    [:div {:class "card-meta flex items-center gap-2 text-xs text-gray-600"}
     [:svg {:class "w-4 h-4" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2"}
      [:path {:d "M8 2v4"}]
      [:path {:d "M16 2v4"}]
      [:rect {:width "18" :height "18" :x "3" :y "4" :rx "2"}]
      [:path {:d "M3 10h18"}]]
     [:span (:page/date post "")]]]])

(defn tag-filter [all-tags]
  [:div {:class "section-block"}
   [:h3 {:class "section-label"} "Filter by tags"]
   [:div {:class "flex flex-wrap gap-2" :id "tag-filter"}
    (for [tag all-tags]
      [:button {:class "tag-btn px-3 py-1.5 text-sm font-medium rounded-full border border-border-default text-gray-600 bg-surface-elevated hover:border-primary-300 hover:text-primary-600 hover:bg-primary-50 transition-all duration-200"
                :data-tag (name tag)}
       (name tag)])]])

;;; ---------------------------------------------------------------------------
;;; Frontpage sections (split to reduce nesting)
;;; ---------------------------------------------------------------------------

(defn frontpage-hero-section []
  [:section {:class "layout-container layout-section-spacing"}
   [:div {:class "flex flex-col lg:flex-row items-center gap-8 lg:gap-12"}
    [:div {:class "w-full lg:w-2/5 flex justify-center lg:justify-start"}
     [:div {:class "hero-decor"}
      [:div {:class "hero-decor-blur"}]
      [:div {:class "hero-avatar"}
       [:svg {:class "w-32 h-32 md:w-40 md:h-40 text-white" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "1.5"}
        [:path {:d "M4 4h16v12H4z"}]
        [:path {:d "M8 20h8"}]
        [:path {:d "M12 16v4"}]]]]]
    [:div {:class "hero-text lg:w-3/5"}
     [:div {:class "hero-title-group"}
      [:h1 {:class "text-3xl md:text-4xl lg:text-5xl font-bold text-gray-900 tracking-tight"}
       "Software Engineer"]
      [:h1 {:class "text-3xl md:text-4xl lg:text-5xl font-bold text-primary-600 tracking-tight"}
       "AI Enthusiast"]
      [:h1 {:class "text-3xl md:text-4xl lg:text-5xl font-bold text-gray-900 tracking-tight"}
       "World Explorer"]]
     [:p {:class "text-lg text-gray-600 max-w-xl mx-auto lg:mx-0 leading-relaxed"}
      "Welcome to my digital garden — a place where I share my thoughts on software engineering, artificial intelligence, and my journey exploring the world."]
     [:div {:class "hero-actions"}
      [:a {:href "#articles" :class "btn btn-primary"}
       "Read Articles"]
      [:a {:href "/about/" :class "btn btn-secondary"}
       "About Me"]]]]])

(defn frontpage-blog-posts-section [db]
  [:section {:id "articles" :class "layout-container layout-section-spacing border-t border-border-default"}
   [:div {:class "section-block"}
    [:h2 {:class "text-2xl font-bold text-gray-900 section-title"} "Articles"]
    [:p {:class "text-gray-600"} "Explore my thoughts on technology, programming, and more."]]
   #_(tag-filter (get-all-tags db))
   [:div {:class "layout-grid-3" :id "posts-container"}
    (for [blog-post (get-blog-posts db)]
      (blog-post-card blog-post))]])

(def frontpage-tag-filter-script
  "(function() {
    const selectedTags = new Set();
    const tagButtons = document.querySelectorAll('#tag-filter button');
    const posts = document.querySelectorAll('.post');

    tagButtons.forEach(function(button) {
      button.addEventListener('click', function() {
        const tag = this.getAttribute('data-tag');

        if (selectedTags.has(tag)) {
          selectedTags.delete(tag);
          this.classList.remove('bg-primary-600', 'text-white', 'border-primary-600', 'hover:bg-primary-700');
          this.classList.add('bg-surface-elevated', 'text-gray-600', 'border-border-default', 'hover:border-primary-300', 'hover:text-primary-600', 'hover:bg-primary-50');
        } else {
          selectedTags.add(tag);
          this.classList.remove('bg-surface-elevated', 'text-gray-600', 'border-border-default', 'hover:border-primary-300', 'hover:text-primary-600', 'hover:bg-primary-50');
          this.classList.add('bg-primary-600', 'text-white', 'border-primary-600', 'hover:bg-primary-700');
        }

        filterPosts();
      });
    });

    function filterPosts() {
      posts.forEach(function(post) {
        const postTags = post.getAttribute('data-tags')?.split(',') || [];
        const isVisible = selectedTags.size === 0 || Array.from(selectedTags).some(function(tag) {
          return postTags.includes(tag);
        });
        post.style.display = isVisible ? 'block' : 'none';
      });
    }
  })();")

(defn render-frontpage [context _page]
  (let [db (:app/db context)]
    (layout
     {:title "LinZiHao's Digital Garden"
      :description "LinZiHao's Digital Garden - Software Engineer, AI Enthusiast, World Explorer"}
     [:div
      [:div {:id "progress-bar"}]
      (frontpage-hero-section)
      (frontpage-blog-posts-section db)
      #_[:script frontpage-tag-filter-script]])))

;;; ---------------------------------------------------------------------------
;;; Article page
;;; ---------------------------------------------------------------------------

(def header
  [:header [:a {:href "/"} "首页"]])

(defn render-article [_context page]
  (layout {}
          header
          (hiccup/unescape (body-to-html-string (:page/body page)))))

;;; ---------------------------------------------------------------------------
;;; Blog post page (split sections to reduce nesting)
;;; ---------------------------------------------------------------------------

(defn blog-post-progress-bar []
  [:div {:id "progress-bar"}])

(defn blog-post-header [page]
  (list
   [:div {:class "flex flex-wrap justify-center gap-2 section-heading-spacing"}
    (for [tag (:blog-post/tags page)]
      [:span {:class "badge badge-neutral"}
       (name tag)])]
   [:h1 {:class "text-3xl md:text-4xl lg:text-5xl font-bold text-gray-900 leading-tight section-heading-spacing heading-center"}
    (:page/title page)]
   (when (:page/description page)
     [:p {:class "text-lg md:text-xl text-gray-600 max-w-2xl mx-auto leading-relaxed section-heading-spacing heading-center"}
      (:page/description page)])
   [:div {:class "flex items-center justify-center gap-4 text-sm text-gray-600 section-meta-spacing"}
    [:div {:class "flex items-center gap-2"}
     [:svg {:class "w-4 h-4" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2"}
      [:path {:d "M8 2v4"}]
      [:path {:d "M16 2v4"}]
      [:rect {:width "18" :height "18" :x "3" :y "4" :rx "2"}]
      [:path {:d "M3 10h18"}]]
     [:span (:page/date page "")]]]))

(defn blog-post-toc [toc]
  (when (seq toc)
    [:nav {:class "toc"}
     [:div {:class "section-label"}
      "Table of Contents"]
     [:ul {:class "space-y-1 text-sm"}
      (for [heading toc]
        [:li (render-toc-item heading)])]]))

(defn blog-post-toc-column [toc]
  [:aside {:class "hidden lg:block w-44 flex-shrink-0"}
   [:div {:class "layout-sidebar-sticky"}
    (blog-post-toc toc)]])

(defn blog-post-content-column [body-html-str]
  [:article {:class "layout-main-content"}
   [:div {:class "prose prose-lg max-w-none"}
    (hiccup/unescape body-html-str)]])

(defn blog-post-backlinks-column []
  [:aside {:class "hidden xl:block w-44 flex-shrink-0"}
   [:div {:class "layout-sidebar-sticky"}
    [:div {:class "section-label"}
     "Backlinks"]
    [:div {:class "text-muted-italic"}
     "Coming soon..."]]])

(defn blog-post-three-columns [toc body-html-str]
  [:div {:class "flex flex-col lg:flex-row gap-6 lg:gap-8 xl:gap-12"}
   (blog-post-toc-column toc)
   (blog-post-content-column body-html-str)
   (blog-post-backlinks-column)])

(defn blog-post-page-body [page body-html-str toc]
  [:div {:class "layout-container layout-section-spacing"}
   [:header {:class "layout-content-header"}
    (blog-post-header page)]
   [:div {:class "mx-auto"}
    (blog-post-three-columns toc body-html-str)]])

(def blog-post-toc-script-content
  (str "(function() {"
       "  const headings = document.querySelectorAll('.prose h2, .prose h3, .prose h4, .prose h5, .prose h6');"
       "  const tocLinks = document.querySelectorAll('.toc a');"
       "  if (headings.length === 0 || tocLinks.length === 0) return;"
       "  function updateActiveHeading() {"
       "    let currentActiveIndex = 0;"
       "    const scrollOffset = 120;"
       "    headings.forEach(function(heading, index) {"
       "      const rect = heading.getBoundingClientRect();"
       "      if (rect.top <= scrollOffset) { currentActiveIndex = index; }"
       "    });"
       "    tocLinks.forEach(function(link, index) {"
       "      if (index === currentActiveIndex) {"
       "        link.classList.add('text-primary-600', 'font-semibold', 'bg-primary-50');"
       "        link.classList.remove('text-gray-600', 'text-gray-600');"
       "      } else {"
       "        link.classList.remove('text-primary-600', 'font-semibold', 'bg-primary-50');"
       "        link.classList.add('text-gray-600');"
       "      }"
       "    });"
       "  }"
       "  document.addEventListener('scroll', updateActiveHeading, { passive: true });"
       "  window.addEventListener('resize', updateActiveHeading);"
       "  updateActiveHeading();"
       "})();"))

(defn blog-post-toc-script []
  [:script blog-post-toc-script-content])

(defn render-blog-post [_context page]
  (let [body-html-str (body-to-html-string (:page/body page))
        headings (extract-headings body-html-str)
        toc (build-toc headings)]
    (layout
     {:title (:page/title page)
      :description (:page/description page "")}
     (blog-post-progress-bar)
     (blog-post-page-body page body-html-str toc)
     (blog-post-toc-script))))

;;; ---------------------------------------------------------------------------
;;; Main entry
;;; ---------------------------------------------------------------------------

(def !debug nil)

(defn render-page [context page]
  (let [result (case (:page/kind page)
                 :page.kind/frontpage (render-frontpage context page)
                 :page.kind/blog-post (render-blog-post context page)
                 :page.kind/article (render-article context page))]
    (alter-var-root #'!debug (constantly result))
    (lint/run-lint result)
    result))
