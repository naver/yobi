/**
 * @(#)hive.Markdown 2013.03.21
 *
 * Copyright NHN Corporation.
 * Released under the MIT license
 * 
 * http://hive.dev.naver.com/license
 */
hive.Markdown = function(htOptions){

	var htVar = {};
	var htElement = {};
	
	/**
	 * initialize
	 * @param {Hash Table} htOptions
	 */
	function _init(htOptions){
		_initVar(htOptions);
		_initElement(htOptions);
		
		_enableMarkdown();
	}

    /**
     * Return a regular expresion for autolink.
     */
    function _rxLink() {
        // case insensitive match
        var sUserPat = "[a-z0-9-_.]+";
        var sProjectPat = "[-a-z0-9_]+";
        var sNumberPat = "[0-9]+";
        var sShaPat = "[0-9a-f]{7,40}";

        var sProjectPathPat = sUserPat + "/" + sProjectPat;
        var sTargetPat =
            "#(" + sNumberPat + ")|(@)?(" + sShaPat + ")|@(" + sUserPat + ")";

        return new RegExp(
                "(\\S*?)(" + sProjectPathPat + ")?(?:" + sTargetPat + ")(\\S*?)", "gi");
    }

	/**
	 * initialize variables
	 * @param {Hash Table} htOptions
	 */
	function _initVar(htOptions){
		htVar.rxCodeBlock = /```(\w+)(?:\r\n|\r|\n)((\r|\n|.)*?)(\r|\n)```/gm;
        htVar.rxLink = _rxLink();
		htVar.sTplSwitch = htOptions.sTplSwitch;
        htVar.sIssuesUrl = htOptions.sIssuesUrl;
        htVar.sProjectUrl = htOptions.sProjectUrl;
	}
	
	/**
	 * initialize element
     * @param {Hash Table} htOptions
	 */
	function _initElement(htOptions){
		htElement.waTarget = $(htOptions.aTarget) || $("[markdown]");
	}
	
	/**
	 * Render as Markdown document
     * @require showdown.js
     * @require hljs.js
	 * @param {String} sText
	 * @return {String}
	 */
	function _renderMarkdown(sText) {
        var converter, sHTML;

		sText = sText.replace(htVar.rxCodeBlock, function(match, p1, p2) {
			try {
				return '<pre><code class="' + p1 + '">' + hljs(p2, p1).value + '</code></pre>';
			} catch (e) {
				return '<pre><code>' + hljs(p2).value + '</code></pre>';
			}
		});

        converter = Markdown.getSanitizingConverter();

        converter.hooks.chain("postBlockGamut", function(sText, runBlockGamut) {
            var makeLink = function(sMatch, sPre, sProject, sNum, sAt, sSha, sUser, sPost) {
                var path, text;

                if (sPost) {
                    return sMatch;
                }

                if (sPre.substr(0, 4).toLowerCase() == 'http') {
                    return sMatch;
                }

                if (sSha && sProject && sAt) {
                    // owner/sProject@2022d330c5858eae9ca9cb5acb9e6a5060563b2c
                    path = '/' + sProject + '/commit/' + sSha;
                    text = sProject + '/' + sSha;
                } else if (sSha && !sAt) {
                    // 2022d330c5858eae9ca9cb5acb9e6a5060563b2c
                    path = htVar.sProjectUrl + '/commit/' + sSha;
                    text = sSha;
                } else if (sSha && sAt) {
                    // @abc1234
                    // This is a link for sUser even if it looks like a 160bit sSha.
                    path = '/' + sSha;
                    text = '@' + sSha;
                } else if (sNum && sProject) {
                    // owner/sProject#1234
                    path = '/' + sProject + '/issue/' + sNum;
                    text = sProject + '/' + sNum;
                } else if (sNum) {
                    // #1234
                    path = htVar.sProjectUrl + '/issue/' + sNum;
                    text ='#' + sNum;
                } else if (sUser) {
                    // @foo
                    path = '/' + sUser;
                    text = '@' + sUser;
                }

                if (path && text) {
                    return sPre + '<a href="' + path + '">' + text + '</a>' + sPost;
                } else {
                    return sMatch;
                }
            }

            return sText.replace(htVar.rxLink, makeLink);
        });

        sHTML = converter.makeHtml(sText);

        return sHTML;
	}

	/**
	 * set Markdown Editor
	 * @param {Wrapped Element} welTextarea
	 */
	function _setEditor(welTextarea) {
		// create new preview area 
		var welPreview = $('<div class="markdown-preview">');
		welPreview.css({
			"display"   : "none",
			"width"     : welTextarea.width()  + 'px',
			"min-height": welTextarea.height() + 'px',
			"padding"   : welTextarea.css("padding")
		});

		var welPreviewSwitch = $('<div id="mode-select">');
			welPreviewSwitch.html(htVar.sTplSwitch);

		var fOnChangeSwitch = function() {
			var bPreview = ($("input:radio[name=edit-mode]:checked").val() == "preview");
			welPreview.html(_renderMarkdown(welTextarea.val()));
			(bPreview ? welPreview: welTextarea).show();
			(bPreview ? welTextarea: welPreview).hide();
		};
		welPreviewSwitch.change(fOnChangeSwitch);

		welTextarea.before(welPreviewSwitch);
		welTextarea.before(welPreview);
	}

	/**
	 * set Markdown Viewer
	 * @param {Wrapped Element} welTarget is not <textarea> or <input>
	 */
	function _setViewer(welTarget) {
		welTarget.html(_renderMarkdown(welTarget.text())).show();
	}

	/**
	 * enableMarkdown
	 * same as nforge.markdown.enable
	 */
	function _enableMarkdown(){
		var sTagName;
		
		htElement.waTarget.each(function(nIndex, elTarget){
			sTagName = elTarget.tagName.toUpperCase();
			
			if(sTagName == "TEXTAREA" || sTagName == "INPUT" || elTarget.contentEditable == "true"){
				_setEditor($(elTarget));
			} else {
				_setViewer($(elTarget));
			}
		});
	}

	// initialize
	_init(htOptions || {});
};
