var MAIDSafe = {};
MAIDSafe.loadRepoOwners = function(repoSelect) {
	var ownerSelect = jQuery(':input:eq('
			+ (jQuery(':input').index(repoSelect) + 1) + ')');
	var setOwners = function(data) {
		var options = '';
		options += '<option id="' + MAIDSafe.org + '">' + MAIDSafe.org
				+ '</option>';
		for (var i = 0; i < data.length; i++) {
			options += '<option id="' + data[i].owner.login + '">'
					+ data[i].owner.login + '</option>';
		}
		ownerSelect.append(options);
		MAIDSafe.loadBranches(ownerSelect);
	};
	ownerSelect.children().remove();
	MAIDSafe.getForks(repoSelect.val(), setOwners);
};
MAIDSafe.loadBranches = function(element) {
	var ownerSelect = jQuery(element);
	var repoSelect = jQuery(':input:eq('
			+ (jQuery(':input').index(ownerSelect) - 1) + ')');
	var branchesSelect = jQuery(':input:eq('
			+ (jQuery(':input').index(ownerSelect) + 1) + ')');
	var setBranches = function(data) {
		var options = '';
		for (var i = 0; i < data.length; i++) {
			options += '<option id="' + data[i].name + '">' + data[i].name
					+ '</option>';
		}
		branchesSelect.append(options);
	};
	branchesSelect.children().remove();
	MAIDSafe.getBranches(repoSelect.val(), ownerSelect.val(), setBranches);
};
MAIDSafe.AddClickObserver = function(repoSelectElement) {
	this.register = function() {		
		setTimeout(function() {
			console.log(jQuery(':input:eq('
					+ (jQuery(':input').index(repoSelectElement) + 5) + ')'));
			MAIDSafe.loadRepos(jQuery(':input:eq('
					+ (jQuery(':input').index(repoSelectElement) + 5) + ')'));
		}, 100);
	};
};
MAIDSafe.registerEvents = function(element) {	
	var buttons;
	jQuery(element).on('change', function(event) {
		MAIDSafe.loadRepoOwners(jQuery(event.target));
	});
	jQuery(':input:eq(' + (jQuery(':input').index(element) + 1) + ')').on(
			'change', function(event) {
				MAIDSafe.loadBranches(jQuery(event.target));
			});
	buttons = jQuery('button');
	jQuery(buttons[buttons.length - 3]).on('click', new MAIDSafe.AddClickObserver(element).register);
};
MAIDSafe.loadRepos = function(element) {
	var options = '';
	if (!MAIDSafe.hasOwnProperty('repos')) {
		return;
	}
	for (var i = 0; i < MAIDSafe.repos.length; i++) {
		options += '<option id="' + MAIDSafe.repos[i].name + '">'
				+ MAIDSafe.repos[i].name + '</option>';
	}
	if (!element) {
		element = jQuery('select[repo="true"]');
	}
	jQuery(element).append(options);
	MAIDSafe.registerEvents(element);
	MAIDSafe.loadRepoOwners(element);
};
MAIDSafe.getRepositories = function() {
	jQuery.ajax({
		url : 'https://api.github.com/orgs/' + MAIDSafe.org + '/repos',
		crossDomain : true,
		headers : {
			'Authorization' : 'token ' + MAIDSafe.token
		}
	}).success(function(data) {
		MAIDSafe.repos = data;
		MAIDSafe.loadRepos();
	})
};
MAIDSafe.getForks = function(repoName, callback) {
	jQuery.ajax(
			{
				url : 'https://api.github.com/repos/' + MAIDSafe.org + '/'
						+ repoName + '/forks',
				crossDomain : true,
				headers : {
					'Authorization' : 'token ' + MAIDSafe.token
				}
			}).success(callback);
};
MAIDSafe.getBranches = function(repo, owner, callback) {
	jQuery.ajax(
			{
				url : 'https://api.github.com/repos/' + owner + '/' + repo
						+ '/branches',
				crossDomain : true,
				headers : {
					'Authorization' : 'token ' + MAIDSafe.token
				}
			}).success(callback);
};
jQuery('document').ready(function() {
	var element = jQuery('span#tempStore');
	MAIDSafe.token = element.text();
	element.remove();
	element = jQuery('span#tempOrg');
	MAIDSafe.org = element.text();
	element.remove();
	MAIDSafe.getRepositories();
});
