(use 'clj-webdriver.taxi)

(set-driver! {:browser :firefox} "http://www.nfl.com/scores/2011/REG10")
(to (attribute "a.game-center-link" :href))
(click {:xpath "//a[@href='#analyze']"})
(pprint (html {:xpath "//tr[@class='tbdy1']/td/a"}))
(pprint (text {:xpath "//tr[@class='tbdy1']/td/a"}))
(close)


