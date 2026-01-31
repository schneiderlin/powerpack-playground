(ns powerblog.core
  (:require
   [clojure.string :as str]
   [datomic.api :as d]
   [powerpack.hiccup :as hiccup]
   [powerpack.markdown :as md]
   [powerblog.components :as components]
   [powerblog.lint :as lint]))

(comment
  (require '[powerpack.dev :as dev])
  (def app (dev/get-app))
  (def db (d/db (:datomic/conn app)))
  :rcf)

(defn get-page-kind [file-name]
  (cond
    (re-find #"^blog-posts/" file-name)
    :page.kind/blog-post

    (re-find #"^index\.md" file-name)
    :page.kind/frontpage

    (re-find #"\.md$" file-name)
    :page.kind/article))

(defn create-tx [file-name txes]
  (let [kind (get-page-kind file-name)]
    (for [tx txes]
      (cond-> tx
        (and (:page/uri tx) kind)
        (assoc :page/kind kind)))))

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

(comment
  (get-posts-by-tag db :clojure)
  :rcf)

(defn get-all-tags [db]
  (->> (d/q '[:find [?tag ...]
              :where
              [_ :blog-post/tags ?tag]]
            db)
       set))

(defn blog-post-card [post]
  [:a {:href (:page/uri post)
       :class "card card-hoverable block post"
       :data-tags (clojure.string/join "," (:blog-post/tags post))}
   [:div {:class "p-6 space-y-4"}
    ;; Title
    [:h3 {:class "card-title text-lg leading-tight"}
     (:page/title post)]
    ;; Tags
    [:div {:class "flex flex-wrap gap-2"}
     (for [tag (take 3 (:blog-post/tags post))]
       [:span {:class "badge badge-neutral"}
        (name tag)])]
    ;; Description
    [:p {:class "text-base text-gray-600 leading-relaxed line-clamp-3"}
     (:page/description post "")]
    ;; Date
    [:div {:class "card-meta flex items-center gap-2 text-sm"}
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
      [:button {:class "tag-btn px-3 py-1.5 text-sm rounded-full"
                :data-tag (name tag)}
       (name tag)])]])

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
                     "block py-2 px-3 rounded-md text-gray-600 hover:text-primary-600 hover:bg-primary-50 transition-all duration-300"
                     "block py-1 pl-6 text-sm text-gray-600 hover:text-primary-600 transition-colors duration-300")]
    (list
     [:a {:href (str "#" (:slug heading))
          :class base-class
          :data-heading (:slug heading)}
      (:text heading)]

     (when (seq (:subheadings heading))
       [:div
        (for [sub (:subheadings heading)]
          (render-toc-item sub))]))))

(defn layout [{:keys [title description lang]} & content]
  [:html {:lang (or lang "en")}
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:meta {:name "description" :content (or description "Blog")}]
    (when title [:title title])
    [:link {:rel "icon" :type "image/svg+xml" :href "/favicon.svg"}]
    ;; Design System CSS
    [:link {:rel "stylesheet" :href "/css/styles.css"}]
    ;; Progress bar styles
    [:style "
      #progress-bar {
        --progress: 0;
        position: fixed;
        top: 0;
        left: 0;
        height: 2px;
        width: var(--progress);
        background: linear-gradient(90deg, var(--moss-500), var(--moss-600));
        z-index: 9999;
        transition: width 0.15s ease-out;
      }
      .line-clamp-3 {
        display: -webkit-box;
        -webkit-line-clamp: 3;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }
    "]]
   [:body {:class "min-h-screen flex flex-col"}
    components/navbar
    [:main {:class "flex-1"} content]
    components/footer
    [:script components/navbar-script]
    ;; Progress bar script
    [:script "
      (function() {
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
      })();
    "]]])

(def header
  [:header [:a {:href "/"} "首页"]])

(defn render-frontpage [context page]
  (layout
   {:title "LinZiHao's Digital Garden"
    :description "LinZiHao's Digital Garden - Software Engineer, AI Enthusiast, World Explorer"}
   [:div
    ;; Progress bar
    [:div {:id "progress-bar"}]

    ;; Hero section - Wabi-Sabi style
    [:section {:class "hero-section layout-container layout-section-spacing"}
     [:div {:class "flex flex-col lg:flex-row items-center gap-10 lg:gap-16"}
      ;; Left: Avatar/Visual with organic shape
      [:div {:class "w-full lg:w-2/5 flex justify-center lg:justify-start lg:pl-4"}
       [:div {:class "hero-decor"}
        [:div {:class "hero-decor-blur"}]
        [:div {:class "hero-avatar"}
         [:svg {:class "w-28 h-28 md:w-36 md:h-36 text-white opacity-90" :viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "1.5"}
          [:path {:d "M4 4h16v12H4z"}]
          [:path {:d "M8 20h8"}]
          [:path {:d "M12 16v4"}]]]]]

      ;; Right: Text content - asymmetric
      [:div {:class "hero-text lg:w-3/5 lg:pr-8"}
       [:div {:class "hero-title-group animate-ink-spread"}
        [:h1 {:class "hero-title text-4xl md:text-5xl lg:text-6xl"}
         "Software Engineer"]
        [:h1 {:class "hero-title hero-title-accent text-4xl md:text-5xl lg:text-6xl"}
         "AI Enthusiast"]
        [:h1 {:class "hero-title text-4xl md:text-5xl lg:text-6xl"}
         "World Explorer"]]
       [:p {:class "hero-subtitle mt-6 max-w-lg mx-auto lg:mx-0"}
        "Welcome to my digital garden — a place where I share my thoughts on software engineering, artificial intelligence, and my journey exploring the world."]
       [:div {:class "hero-actions"}
        [:a {:href "#articles" :class "btn btn-primary"}
         "Read Articles"]
        [:a {:href "/about" :class "btn btn-secondary"}
         "About Me"]]]]]

    ;; Blog posts section - organic grid
    [:section {:id "articles" :class "layout-container layout-section-spacing border-t border-border-default"}
     [:div {:class "section-block"}
      [:h2 {:class "section-title text-3xl md:text-4xl"} "Articles"]
      [:p {:class "section-description"} "Explore my thoughts on technology, programming, and more."]]
     (tag-filter (get-all-tags (:app/db context)))
     [:div {:class "layout-grid-3" :id "posts-container"}
      (for [blog-post (get-blog-posts (:app/db context))]
        (blog-post-card blog-post))]]]

   ;; Tag filter script
   [:script "
     (function() {
       const selectedTags = new Set();
       const tagButtons = document.querySelectorAll('#tag-filter button');
       const posts = document.querySelectorAll('.post');

       tagButtons.forEach(function(button) {
         button.addEventListener('click', function() {
           const tag = this.getAttribute('data-tag');

           if (selectedTags.has(tag)) {
             selectedTags.delete(tag);
             this.classList.remove('active');
           } else {
             selectedTags.add(tag);
             this.classList.add('active');
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
     })();
   "]))

(defn render-article [context page]
  (layout {}
          header
          (hiccup/unescape (body-to-html-string (:page/body page)))))

(defn blog-post-progress-bar []
  [:div {:id "progress-bar"}])

(defn blog-post-header [page]
  (list
   ;; Tags
   [:div {:class "flex flex-wrap justify-center gap-2 section-heading-spacing"}
    (for [tag (:blog-post/tags page)]
      [:span {:class "badge badge-neutral"}
       (name tag)])]
   ;; Title
   [:h1 {:class "text-3xl md:text-4xl lg:text-5xl leading-tight section-heading-spacing heading-center font-normal"}
    (:page/title page)]
   ;; Description
   (when (:page/description page)
     [:p {:class "text-lg md:text-xl text-gray-600 max-w-2xl mx-auto leading-relaxed section-heading-spacing heading-center"}
      (:page/description page)])
   ;; Date
   [:div {:class "flex items-center justify-center gap-4 text-base text-gray-600 section-meta-spacing"}
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
     [:ul {:class "space-y-1 text-base"}
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
       "        link.classList.add('text-primary-600', 'font-medium', 'bg-primary-50');"
       "        link.classList.remove('text-gray-600', 'text-gray-600');"
       "      } else {"
       "        link.classList.remove('text-primary-600', 'font-medium', 'bg-primary-50');"
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

(defn render-blog-post [context page]
  (let [body-html-str (body-to-html-string (:page/body page))
        headings (extract-headings body-html-str)
        toc (build-toc headings)]
    (layout
     {:title (:page/title page)
      :description (:page/description page "")}
     (blog-post-progress-bar)
     (blog-post-page-body page body-html-str toc)
     (blog-post-toc-script))))

(defn render-page [context page]
  (let [result (case (:page/kind page)
                 :page.kind/frontpage (render-frontpage context page)
                 :page.kind/blog-post (render-blog-post context page)
                 :page.kind/article (render-article context page))]
    (def !debug result)
    (lint/run-lint result)
    result))

(comment
  !debug

  (lint/element? !debug)
  (lint/run-lint !debug)

  (lint/run-lint [:html
                  [:div {}
                   [:div {:class "invalidName"}]
                   [:span {:class "invalidName"}]]])

  :rcf)

(def config
  {:site/title "The Powerblog"
   :datomic/schema-file "resources/schema.edn"
   :powerpack/port 8000
   :powerpack/log-level :debug
   :powerpack/render-page #'render-page
   :powerpack/create-ingest-tx #'create-tx
   :optimus/assets [{:public-dir "public"
                     :paths ["/favicon.svg" "/css/styles.css"]}]
   :optimus/options {:minify-js-assets? false
                     :minify-css-assets? false}})
